package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.service.*;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.dto.request.*;
import net.diveon.backend.support.TestPorts;
import java.nio.file.*;
import java.time.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

/** Disposable loopback-only PostgreSQL; does not bootstrap the app or read deployment configuration. */
@EnabledIfEnvironmentVariable(named="OBO_PG_TEST", matches="local")
class OboDisclosurePostgresTest {
    static Path directory, bin;
    static LocalContainerEntityManagerFactoryBean factory;
    static EntityManager em;
    static TransactionTemplate tx;
    static SolveSubmissionRepository submissions;
    static OboDisclosureService disclosure;
    static ProblemRevisionService revisions;
    static final LocalDateTime now=LocalDateTime.of(2026,10,31,12,0);
    static final String suffix=System.getProperty("os.name").startsWith("Windows")?".exe":"";

    static void command(String... args) throws Exception {
        var process=new ProcessBuilder(args).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("process.log").toFile())).start();
        if(!process.waitFor(180,TimeUnit.SECONDS)){process.destroyForcibly();fail("Local PostgreSQL timed out: "+directory);}
        assertEquals(0,process.exitValue(),"Inspect "+directory.resolve("process.log"));
    }
    @BeforeAll static void start() throws Exception {
        assertNotNull(System.getenv("PG_BIN")); bin=Path.of(System.getenv("PG_BIN"));
        directory=Files.createTempDirectory("obo-disclosure-pg-").toRealPath();
        assertTrue(directory.startsWith(Path.of(System.getProperty("java.io.tmpdir")).toRealPath()));
        int port=TestPorts.freePort();
        try {
            command(bin.resolve("initdb"+suffix).toString(),"-D",directory.resolve("data").toString(),"-U","postgres","-A","trust","--no-locale","-E","UTF8");
            command(bin.resolve("pg_ctl"+suffix).toString(),"-D",directory.resolve("data").toString(),"-l",directory.resolve("server.log").toString(),"-o","-h 127.0.0.1 -p "+port,"-w","start");
            var ds=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:"+port+"/postgres","postgres","");
            factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);
            factory.setPackagesToScan("net.diveon.backend.domain");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create-drop","hibernate.jdbc.time_zone","UTC",
                "hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            factory.afterPropertiesSet();
            new org.springframework.jdbc.core.JdbcTemplate(ds).execute(Files.readString(Path.of("ops/2026-10-31-obo-revisions.sql")));
            new org.springframework.jdbc.core.JdbcTemplate(ds).execute("CREATE TABLE IF NOT EXISTS problem_solver(prob_id bigint,user_id bigint,PRIMARY KEY(prob_id,user_id))");
            var emf=factory.getObject();em=SharedEntityManagerCreator.createSharedEntityManager(emf);
            tx=new TransactionTemplate(new JpaTransactionManager(emf));
            var repositories=new JpaRepositoryFactory(em);
            revisions=new ProblemRevisionService(repositories.getRepository(net.diveon.backend.domain.problem.repository.ProblemRevisionRepository.class),
                repositories.getRepository(net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository.class),
                repositories.getRepository(net.diveon.backend.domain.problem.repository.ProblemCodingRepository.class),new com.fasterxml.jackson.databind.ObjectMapper());
            submissions=repositories.getRepository(SolveSubmissionRepository.class);
            disclosure=new OboDisclosureService(repositories.getRepository(ContestProblemRepository.class),
                repositories.getRepository(ContestParticipantRepository.class),submissions,
                Clock.fixed(now.toInstant(ZoneOffset.UTC),ZoneOffset.UTC));
        }catch(Throwable error){try{stop();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    @AfterAll static void stop() throws Exception {
        if(factory!=null){factory.destroy();factory=null;}
        if(directory!=null&&Files.exists(directory.resolve("data/postmaster.pid")))
            command(bin.resolve("pg_ctl"+suffix).toString(),"-D",directory.resolve("data").toString(),"-m","fast","-w","stop");
        if(directory!=null)System.out.println("OBO disposable PostgreSQL stopped; logs: "+directory);
    }
    record Fixture(User user,Problem problem,SolveSubmission submission) {}
    Fixture seed(SolveSubmission.SubmissionState state){
        String id=java.util.UUID.randomUUID().toString();
        var user=new User(id,"unused",id,id+"@example.test",null,null);em.persist(user);
        var problem=new Problem(user,"objective","OBO","OS","easy","public");em.persist(problem);
        var submission=new SolveSubmission(problem,user);submission.setSubmissionState(state);em.persist(submission);em.flush();
        return new Fixture(user,problem,submission);
    }
    Contest attach(Fixture f,LocalDateTime start,LocalDateTime end,boolean submit){
        var contest=new Contest(f.user(),null,"OBO contest","test",Contest.ContestType.INDIVIDUAL,
            Contest.ParticipationType.INDIVIDUAL,Contest.Visibility.PUBLIC,start,end,null,null,null);em.persist(contest);
        var link=new ContestProblem(contest,f.problem(),100);em.persist(link);
        em.persist(new ContestParticipant(contest,f.user(),ContestParticipant.ContestRole.PARTICIPANT));
        if(submit)em.persist(new ContestSubmission(contest,link,f.user(),f.submission(),"COMPLETED"));
        em.flush();return contest;
    }
    @Test void onlyOwnCompletedSubmissionQualifies(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.PENDING);
        assertEquals(0,submissions.countDisclosureEligible(f.user().getId(),f.problem().getId(),now));
        f.submission().setSubmissionState(SolveSubmission.SubmissionState.FAILED);em.flush();
        assertEquals(0,submissions.countDisclosureEligible(f.user().getId(),f.problem().getId(),now));
        f.submission().setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);em.flush();
        assertEquals(1,submissions.countDisclosureEligible(f.user().getId(),f.problem().getId(),now));
        assertEquals(0,submissions.countDisclosureEligible(-1L,f.problem().getId(),now));
        status.setRollbackOnly();
    });}
    @Test void contestSubmissionWaitsUntilExactEnd(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.COMPLETED);var contest=attach(f,now.minusHours(1),now.plusSeconds(1),true);
        assertEquals(0,submissions.countDisclosureEligible(f.user().getId(),f.problem().getId(),now));
        contest.updateTime(now.minusHours(1),now);em.flush();
        assertEquals(1,submissions.countDisclosureEligible(f.user().getId(),f.problem().getId(),now));
        status.setRollbackOnly();
    });}
    @Test void normalCompletionCannotBypassTwoActiveContestMemberships(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.COMPLETED);
        var first=attach(f,now.minusHours(1),now.plusHours(1),false);
        var second=attach(f,now.minusHours(1),now.plusHours(2),false);
        assertFalse(disclosure.canReadExplanation(f.user().getId(),f.problem().getId()));
        first.updateTime(now.minusHours(1),now);em.flush();
        assertFalse(disclosure.canReadExplanation(f.user().getId(),f.problem().getId()));
        second.updateTime(now.minusHours(1),now);em.flush();
        assertTrue(disclosure.canReadExplanation(f.user().getId(),f.problem().getId()));
        status.setRollbackOnly();
    });}
    @Test void completedLegacyObjectiveContestNeedsNoGeneralSubmission(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.PENDING);
        var contest=attach(f,now.minusHours(1),now,false);
        var link=em.createQuery("select p from ContestProblem p where p.contest.id=:id",ContestProblem.class)
            .setParameter("id",contest.getId()).getSingleResult();
        em.persist(new ContestSubmission(contest,link,f.user(),"COMPLETED"));em.flush();
        assertTrue(disclosure.canReadExplanation(f.user().getId(),f.problem().getId()));
        status.setRollbackOnly();
    });}
    @Test void revisionAndSubmissionBindingSurviveLaterProblemChanges(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.COMPLETED);
        var objective=new net.diveon.backend.domain.problem.entity.ProblemObjective(f.problem(),"summary","before",
            java.util.List.of(new net.diveon.backend.domain.problem.others.ForDtoChoice(1,"A",null)),java.util.List.of(1),false,false);
        em.persist(objective);em.flush();
        var first=revisions.capture(f.problem());
        var bound=new SolveSubmission(f.problem(),f.user());bound.bindRevision(first);em.persist(bound);
        objective.updateProblemObjective("summary","after");
        var second=revisions.capture(f.problem());em.flush();
        assertNotEquals(first.getId(),second.getId());
        assertEquals("before",bound.getProblemRevision().getSnapshot().path("description").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode)first.getSnapshot()).put("description","tampered");
        assertEquals("before",first.getSnapshot().path("description").asText());
        assertThrows(IllegalStateException.class,()->bound.bindRevision(second));
        assertEquals(second.getId(),revisions.capture(f.problem()).getId());
        em.clear();
        assertEquals(first.getId(),em.find(SolveSubmission.class,bound.getId()).getProblemRevision().getId());
        status.setRollbackOnly();
    });}
    @Test void databaseAlsoRejectsRevisionMutation(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.COMPLETED);
        var revision=new net.diveon.backend.domain.problem.entity.ProblemRevision("fixture:"+f.problem().getId(),f.problem().getId(),
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("v",1));
        em.persist(revision);em.flush();
        assertThrows(RuntimeException.class,()->em.createNativeQuery("UPDATE problem_revision SET snapshot='{}'::jsonb WHERE id=:id")
            .setParameter("id",revision.getId()).executeUpdate());
        status.setRollbackOnly();
    });}

    @Test void browserAuthoredProblemPersistsAndOldSubmissionSurvivesRemoval() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var request=mapper.readValue(getClass().getResourceAsStream("/obo/browser-coding-request.json"),ProblemCreateCodingRequest.class);
        tx.executeWithoutResult(status->{
            var f=seed(SolveSubmission.SubmissionState.PENDING);
            var repositories=new JpaRepositoryFactory(em);
            var problems=repositories.getRepository(ProblemRepository.class);
            var objectives=repositories.getRepository(ProblemObjectiveRepository.class);
            var codings=repositories.getRepository(ProblemCodingRepository.class);
            var users=repositories.getRepository(net.diveon.backend.domain.user.repository.UserRepository.class);
            var validator=new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");
            var s3=mock(S3Service.class);
            var create=new ProblemCreateService(objectives,mock(ProblemPracticeRepository.class),codings,problems,users,
                mock(OboStepRepository.class),s3,mock(net.diveon.backend.domain.group.repository.GroupRepository.class),
                mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),mock(ContestRepository.class),
                mock(ContestProblemRepository.class),mock(ContestParticipantRepository.class),new PracticeBuildProperties(),Clock.systemUTC(),validator,revisions);
            create.createCoding(f.user().getId(),request);em.flush();
            var problem=em.createQuery("select p from Problem p where p.author.id=:user and p.type='coding'",Problem.class)
                .setParameter("user",f.user().getId()).getSingleResult();
            var revision=revisions.capture(problem);
            var bound=new SolveSubmission(problem,f.user());bound.bindRevision(revision);
            bound.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);em.persist(bound);em.flush();
            var access=new ProblemAccessService(problems,mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),
                mock(net.diveon.backend.domain.group.repository.GroupUserRepository.class),repositories.getRepository(ContestProblemRepository.class),
                repositories.getRepository(ContestParticipantRepository.class));
            var obo=new ProblemOboService(problems,objectives,codings,access,disclosure,submissions,repositories.getRepository(ContestSubmissionRepository.class),repositories.getRepository(SolveResultRepository.class));
            assertEquals(request.getOboJson(),obo.getAuthorOboJson(f.user().getId(),problem.getId()));
            String editVersion=(String)obo.getAuthorSettings(f.user().getId(),problem.getId()).get("editVersion");
            var update=new ProblemUpdateService(problems,objectives,mock(ProblemPracticeRepository.class),codings,
                mock(OboStepRepository.class),users,s3,validator,revisions);
            var keep=new ProblemUpdateCodingRequest();
            keep.setTitle(problem.getTitle());keep.setSummary(request.getSummary());keep.setDescription(request.getDescription());keep.setDifficulty(problem.getDifficulty());
            keep.setExpectedEditVersion(editVersion);
            update.updateProblemCoding(f.user().getId(),problem.getId(),keep);
            assertEquals(revision.getId(),revisions.capture(problem).getId());
            keep.setOboAction("remove");update.updateProblemCoding(f.user().getId(),problem.getId(),keep);em.flush();em.clear();
            assertNull(obo.getAuthorOboJson(f.user().getId(),problem.getId()));
            assertThrows(ProblemEditVersion.EditConflict.class,()->update.updateProblemCoding(f.user().getId(),problem.getId(),keep));
            assertNull(obo.getAuthorOboJson(f.user().getId(),problem.getId()),"stale edit cannot restore removed content");
            var released=obo.getSubmissionObo(f.user().getId(),bound.getId());
            assertEquals(revision.getId(),released.revisionId());
            assertEquals("simulation",released.obo().path("mode").asText());
            assertEquals("ST2",released.obo().path("simulation").path("instances").get(0).path("scenarioId").asText());
            assertEquals("obo-v1",em.find(SolveSubmission.class,bound.getId()).getProblemRevision().getSnapshot().path("judgeProfile").asText());
            verify(s3,times(1)).uploadCodingTestcases(eq(problem.getId()),anyList());
            status.setRollbackOnly();
        });
    }

    @Test void mvcAuthoringSubmissionGradingAndRevisionDisclosureUseRealDatabase() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var fixture=mapper.readTree(getClass().getResourceAsStream("/obo/browser-coding-request.json"));
        tx.executeWithoutResult(status->{try {
            var author=seed(SolveSubmission.SubmissionState.PENDING).user();
            var student=seed(SolveSubmission.SubmissionState.PENDING).user();
            var repo=new JpaRepositoryFactory(em);
            var problems=repo.getRepository(ProblemRepository.class);var objectives=repo.getRepository(ProblemObjectiveRepository.class);
            var codings=repo.getRepository(ProblemCodingRepository.class);var practices=repo.getRepository(ProblemPracticeRepository.class);
            var users=repo.getRepository(net.diveon.backend.domain.user.repository.UserRepository.class);
            var results=repo.getRepository(SolveResultRepository.class);
            var objectiveSubs=repo.getRepository(SolveSubmissionObjectiveRepository.class);
            var codingSubs=repo.getRepository(SolveSubmissionCodingRepository.class);
            var practiceSubs=repo.getRepository(SolveSubmissionPracticeRepository.class);
            var access=new ProblemAccessService(problems,mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),
                mock(net.diveon.backend.domain.group.repository.GroupUserRepository.class),repo.getRepository(ContestProblemRepository.class),repo.getRepository(ContestParticipantRepository.class));
            var validator=new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");
            var s3=mock(S3Service.class);
            var create=new ProblemCreateService(objectives,practices,codings,problems,users,mock(OboStepRepository.class),s3,
                mock(net.diveon.backend.domain.group.repository.GroupRepository.class),mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),
                mock(ContestRepository.class),mock(ContestProblemRepository.class),mock(ContestParticipantRepository.class),new PracticeBuildProperties(),Clock.systemUTC(),validator,revisions);
            var starter=new SubmissionGradeStarterService(results,submissions,objectiveSubs,codingSubs,practiceSubs,users,problems,codings,objectives,practices,access,revisions);
            var grader=new SubmissionGradeAsyncService(users,problems,objectives,codings,practices,submissions,objectiveSubs,codingSubs,practiceSubs,
                results,mock(CodingGradeQueueService.class),mock(net.diveon.backend.domain.user.service.UserTierService.class));
            var submit=new SubmissionGradeService(starter,grader,new JpaTransactionManager(factory.getObject()),true);
            var resultService=new SubmissionResultService(submissions,results,objectiveSubs,codingSubs,repo.getRepository(SolveResultCodingRepository.class));
            var obo=new ProblemOboService(problems,objectives,codings,access,disclosure,submissions,repo.getRepository(ContestSubmissionRepository.class),results);
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new net.diveon.backend.domain.problem.controller.ProblemCreateController(create,mock(ProblemDeleteService.class)),
                new net.diveon.backend.domain.grade.controller.SubmissionGradeController(submit,new SubmissionStatusService(submissions,results),resultService),
                new net.diveon.backend.domain.problem.controller.ProblemOboController(obo))
                .setControllerAdvice(new net.diveon.backend.global.exception.GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper))
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()).build();
            var body=mapper.createObjectNode().put("title","HTTP objective").put("summary","test").put("description","two answers")
                .put("category","kernel").put("difficulty","1").put("visibility","public");
            body.putArray("choices").addObject().put("id","choice_a").put("index",1).put("content","A");
            ((com.fasterxml.jackson.databind.node.ArrayNode)body.get("choices")).addObject().put("id","choice_b").put("index",2).put("content","B");
            body.putArray("answer").add(1).add(2);body.set("oboJson",fixture.get("oboJson").deepCopy());
            var question=(com.fasterxml.jackson.databind.node.ObjectNode)body.path("oboJson").path("simulation").path("question");
            question.put("selectionMode","multiple").put("judgeProfile","text-v1").putNull("gradingInstanceId");
            authenticate(author.getId());
            var created=mvc.perform(post("/api/problems/objective").contentType("application/json").content(body.toString())).andExpect(status().isCreated()).andReturn();
            long problemId=mapper.readTree(created.getResponse().getContentAsString()).path("data").path("probId").asLong();
            assertTrue(problemId>0);
            final long id=problemId;
            // Real MVC + PostgreSQL roundtrip of every currently authored free-diagram template.
            var diagrams=mapper.readTree(getClass().getResourceAsStream("/obo/integration-diagrams.json"));
            for(var diagram:diagrams.path("fixtures")){
                var freeBody=body.deepCopy();
                var free=mapper.createObjectNode().put("mode","per_choice");
                free.putObject("perChoice").set("choice_1",diagram.path("blob").deepCopy());
                freeBody.set("oboJson",free);
                var saved=mvc.perform(post("/api/problems/objective").contentType("application/json").content(freeBody.toString()))
                    .andExpect(status().isCreated()).andReturn();
                long freeId=mapper.readTree(saved.getResponse().getContentAsString()).path("data").path("probId").asLong();
                em.flush();em.clear();
                assertEquals(free,objectives.findById(freeId).orElseThrow().getOboJsonData(),diagram.path("id").asText());
                long count=problems.count();
                var invalid=freeBody.deepCopy();
                ((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("oboJson").path("perChoice").path("choice_1"))
                    .putArray("edges").addObject().put("id","bad").put("source","missing").put("target","missing");
                mvc.perform(post("/api/problems/objective").contentType("application/json").content(invalid.toString()))
                    .andExpect(status().isBadRequest());
                assertEquals(count,problems.count(),"invalid creation must not persist a partial problem");
            }
            authenticate(student.getId());
            mvc.perform(get("/api/challenges/obo/author/settings").param("id",String.valueOf(id))).andExpect(status().isNotFound());
            mvc.perform(get("/api/challenges/obo").param("id",String.valueOf(id))).andExpect(status().isNotFound());
            mvc.perform(post("/api/submissions/grade").contentType("application/json").content("{\"probId\":"+id+",\"answer\":[1,1]}"))
                .andExpect(status().isBadRequest());
            var accepted=mvc.perform(post("/api/submissions/grade").contentType("application/json").content("{\"probId\":"+id+",\"answer\":[2,1]}"))
                .andExpect(status().isOk()).andReturn();
            long submissionId=mapper.readTree(accepted.getResponse().getContentAsString()).path("data").path("submissionId").asLong();
            mvc.perform(get("/api/submissions/"+submissionId+"/result")).andExpect(status().isOk()).andExpect(jsonPath("$.data.isCorrect").value(true));
            var released=mvc.perform(get("/api/challenges/obo/submission").param("id",String.valueOf(submissionId)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.choices[0].id").value("choice_a")).andExpect(jsonPath("$.obo.mode").value("simulation")).andReturn();
            var frozen=mapper.readTree(released.getResponse().getContentAsString());
            var current=objectives.findById(id).orElseThrow();current.setOboJsonData(null);revisions.capture(problems.findForRevision(id).orElseThrow());em.flush();
            mvc.perform(get("/api/challenges/obo/submission").param("id",String.valueOf(submissionId)))
                .andExpect(jsonPath("$.revisionId").value(frozen.path("revisionId").asText())).andExpect(jsonPath("$.obo.mode").value("simulation"));
            authenticate(author.getId());
            mvc.perform(get("/api/challenges/obo/submission").param("id",String.valueOf(submissionId))).andExpect(status().isNotFound());
            verifyNoInteractions(s3);status.setRollbackOnly();
        }catch(Exception error){throw new RuntimeException(error);}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}});
    }
    @Test void privateAuthorBuildsRemainListedWhilePublicBuildsRequireReady(){tx.executeWithoutResult(status->{
        var f=seed(SolveSubmission.SubmissionState.PENDING);
        String unique="author-build-"+java.util.UUID.randomUUID();
        var another=new User(unique,"unused",unique,unique+"@example.test",null,null);em.persist(another);
        var mine=new java.util.ArrayList<Long>();
        Long readyPublic=null;
        for(String visibility:java.util.List.of("private","public")){
            for(String state:java.util.List.of("PENDING","FAILED","READY")){
                var problem=new Problem(f.user(),"practice",unique,"kernel","1",visibility);em.persist(problem);
                var practice=new net.diveon.backend.domain.problem.entity.ProblemPractice(problem,"summary","description",
                        "ubuntu:22.04",java.util.List.of("cat"),"0.5","512m","hash","source");
                practice.updateImageStatus(state,"registry/repo:prob-"+problem.getId());em.persist(practice);
                mine.add(problem.getId()); // '내가 만든 문제'는 공개 범위와 상관없이 전부
                if("public".equals(visibility)&&"READY".equals(state))readyPublic=problem.getId();
            }
        }
        em.flush();
        var repositories=new JpaRepositoryFactory(em);
        var service=new ProblemListService(repositories.getRepository(net.diveon.backend.domain.user.repository.UserRepository.class),
                repositories.getRepository(ProblemRepository.class),submissions);
        var own=service.listProblems(f.user().getId(),1,unique,null,null,null,false,true);
        assertEquals(java.util.Set.copyOf(mine),own.getProblems().stream().map(p->p.getProbId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(0,service.listProblems(another.getId(),1,unique,null,null,null,false,true).getTotal());
        for(Long userId:java.util.List.of(f.user().getId(),another.getId())){
            var visible=service.listProblems(userId,1,unique,null,null,null,false,false);
            assertEquals(java.util.List.of(readyPublic),visible.getProblems().stream().map(p->p.getProbId()).toList());
        }
        status.setRollbackOnly();
    });}

    private static void authenticate(long userId){
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(String.valueOf(userId),null,java.util.List.of()));
    }
}
