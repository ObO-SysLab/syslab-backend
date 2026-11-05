package net.diveon.backend.domain.problem.service;

import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.controller.*;
import net.diveon.backend.domain.problem.dto.request.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.service.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.entity.SolveSubmission;

/** Explicitly enabled, loopback-only fixture server. Real controllers/services/DB; no cloud clients. */
@EnabledIfEnvironmentVariable(named="OBO_HTTP_TEST",matches="local")
class OboLocalHttpTest extends OboDisclosurePostgresTest {
 @Test void serve() throws Exception {
  var mapper=new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  long author=tx.execute(s->seed(SolveSubmission.SubmissionState.PENDING).user().getId());
  long student=tx.execute(s->seed(SolveSubmission.SubmissionState.PENDING).user().getId());
  var repo=new JpaRepositoryFactory(em);
  var problems=repo.getRepository(ProblemRepository.class);var objectives=repo.getRepository(ProblemObjectiveRepository.class);
  var codings=repo.getRepository(ProblemCodingRepository.class);var practices=repo.getRepository(ProblemPracticeRepository.class);
  var users=repo.getRepository(net.diveon.backend.domain.user.repository.UserRepository.class);
  var results=repo.getRepository(SolveResultRepository.class);var objectiveSubs=repo.getRepository(SolveSubmissionObjectiveRepository.class);
  var codingSubs=repo.getRepository(SolveSubmissionCodingRepository.class);var practiceSubs=repo.getRepository(SolveSubmissionPracticeRepository.class);
  var access=new ProblemAccessService(problems,mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),mock(net.diveon.backend.domain.group.repository.GroupUserRepository.class),repo.getRepository(ContestProblemRepository.class),repo.getRepository(ContestParticipantRepository.class));
  var validator=new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");var s3=mock(S3Service.class);
  var create=new ProblemCreateService(objectives,practices,codings,problems,users,mock(OboStepRepository.class),s3,
   mock(net.diveon.backend.domain.group.repository.GroupRepository.class),mock(net.diveon.backend.domain.group.repository.GroupProblemRepository.class),mock(ContestRepository.class),mock(ContestProblemRepository.class),mock(ContestParticipantRepository.class),new PracticeBuildProperties(),Clock.systemUTC(),validator,revisions);
  var update=new ProblemUpdateService(problems,objectives,practices,codings,mock(OboStepRepository.class),users,s3,validator,revisions);
  var detail=new ProblemDetailService(users,problems,objectives,practices,codings,repo.getRepository(OboStepRepository.class),access,disclosure);
  var starter=new SubmissionGradeStarterService(results,submissions,objectiveSubs,codingSubs,practiceSubs,users,problems,codings,objectives,practices,access,revisions);
  var grader=new SubmissionGradeAsyncService(users,problems,objectives,codings,practices,submissions,objectiveSubs,codingSubs,practiceSubs,results,mock(CodingGradeQueueService.class),mock(net.diveon.backend.domain.user.service.UserTierService.class));
  var submit=new SubmissionGradeService(starter,grader,new JpaTransactionManager(factory.getObject()),true);
  var resultService=new SubmissionResultService(submissions,results,objectiveSubs,codingSubs,repo.getRepository(SolveResultCodingRepository.class));
  var obo=new ProblemOboService(problems,objectives,codings,access,disclosure,submissions,repo.getRepository(ContestSubmissionRepository.class),results);
  var mvc=MockMvcBuilders.standaloneSetup(new ProblemCreateController(create,mock(ProblemDeleteService.class)),new ProblemUpdateController(update),new ProblemOboController(obo),
   new ProblemController(detail,mock(ProblemListService.class),mock(ProblemRankingService.class),mock(ProblemRandomService.class),mock(ProblemTodayService.class),mock(ProblemRecommendService.class)),
   new net.diveon.backend.domain.grade.controller.SubmissionGradeController(submit,new SubmissionStatusService(submissions,results),resultService))
   .setControllerAdvice(new net.diveon.backend.global.exception.GlobalExceptionHandler())
   .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper))
   .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()).build();
  var fixtures=mapper.readTree(getClass().getResourceAsStream("/obo/integration-diagrams.json"));
  var b=mapper.createObjectNode().put("title","자원 할당 그래프 · 순환 대기").put("summary","두 프로세스와 두 자원의 관계를 살펴보세요.").put("description","보기 중 교착 상태의 조건을 고르세요. 제출 후 보기별 장면을 확인할 수 있습니다.").put("category","kernel").put("difficulty","1").put("visibility","public");
  b.putArray("choices").addObject().put("id","choice_1").put("index",1).put("content","서로 상대가 가진 자원을 기다립니다.");
  b.withArray("choices").addObject().put("id","choice_2").put("index",2).put("content","모든 자원이 비어 있습니다.");b.putArray("answer").add(1);
  var document=b.putObject("oboJson").put("mode","per_choice").putObject("perChoice");
  for(var f:fixtures.path("fixtures"))if("S3".equals(f.path("id").asText())){document.set("choice_1",f.get("blob"));document.set("choice_2",f.get("blob"));}
  var request=mapper.treeToValue(b,ProblemCreateObjectiveRequest.class);
  long problemId=tx.execute(s->create.createObjective(author,request).getProbId());
  String authorToken=token("obo-local-author"),studentToken=token("obo-local-student");
  long expiresAt=System.currentTimeMillis()+45*60*1000;
  var session=mapper.createObjectNode().put("authorToken",authorToken).put("studentToken",studentToken).put("problemId",problemId).put("expiresAt",expiresAt);
  // Replay captured REAL Linux runs through persisted revisions and the real disclosure API.
  // This test bridge replaces only the cloud queue/result delivery, never the product endpoint.
  var executionFile=Path.of("../obo-harness/evidence/2026-11-01-integration-linux/execution.json");
  var codingDemos=session.putArray("codingDemos");
  if(Files.exists(executionFile)){
   var execution=mapper.readTree(Files.readString(executionFile));
   for(var run:execution.path("outputs")){
    String caseId=run.path("id").asText();
    com.fasterxml.jackson.databind.node.ObjectNode graph=mapper.createObjectNode();
    if(caseId.startsWith("template:")){for(var f:fixtures.path("fixtures"))if(caseId.equals("template:"+f.path("id").asText()))graph=(com.fasterxml.jackson.databind.node.ObjectNode)f.path("blob").deepCopy();}
    else {graph.putArray("edges");var node=graph.putArray("nodes").addObject().put("id","node").put("type",caseId.substring(8));node.putObject("position").put("x",0).put("y",0);node.set("data",fixtures.path("defaults").path(caseId.substring(8)));}
    var body=mapper.createObjectNode().put("title","실제 코드 출력 · "+caseId).put("summary","격리 Linux 실행 기록").put("description","코드가 계산한 두 장면을 재생합니다. 이 로컬 검사는 실행 결과의 보관과 공개를 확인합니다.").put("category","kernel").put("difficulty","1").put("visibility","public").put("inputDescription","그림 JSON").put("outputDescription","v1 JSON 한 줄씩");
    body.putObject("constraints").put("timeLimitMs",2000).put("memoryLimitMb",256).putArray("allowedLanguages").add("python");
    body.putArray("testcases").addObject().put("index",1).put("input",graph.toString()).put("output",run.path("stdout").asText()).put("isSample",true);
    body.withArray("testcases").addObject().put("index",2).put("input","hidden").put("output","hidden").put("isSample",false);
    var schema=body.putObject("oboJson").put("mode","coding_diff").putObject("codingDiff");schema.set("nodes",graph.path("nodes"));schema.set("edges",graph.path("edges"));schema.set("referenceTrace",run.path("trace"));schema.put("sourceTraceText",run.path("stdout").asText());
    var codingRequest=mapper.treeToValue(body,ProblemCreateCodingRequest.class);
    long[] ids=tx.execute(s->{
     long pid=create.createCoding(author,codingRequest).getProbId();var problem=problems.findById(pid).orElseThrow();
     var submission=new SolveSubmission(problem,users.findById(student).orElseThrow());submission.bindRevision(revisions.capture(problem));submission.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);em.persist(submission);em.flush();
     em.persist(new net.diveon.backend.domain.grade.entity.SolveSubmissionCoding(submission,execution.path("source").asText(),"python"));
     // Deliberately WRONG fixture verdict proves successful visual replay cannot turn a grade into CORRECT.
     var result=new net.diveon.backend.domain.grade.entity.SolveResult(submission,net.diveon.backend.domain.grade.entity.SolveResult.SolveResultState.WRONG);em.persist(result);em.flush();
     em.persist(new net.diveon.backend.domain.grade.entity.SolveResultCoding(result,(short)0,0,0,execution.path("source").asText().length()));
     var records=mapper.createArrayNode().add(run.path("record").deepCopy());records.addObject().put("kind","diagram").put("caseIndex",2).put("stdout","HIDDEN_OUTPUT_MUST_NOT_LEAK");
     em.createNativeQuery("UPDATE solve_submission SET obo_public_records=CAST(:records AS jsonb) WHERE id=:id").setParameter("records",records.toString()).setParameter("id",submission.getId()).executeUpdate();em.flush();em.clear();
     return new long[]{pid,submission.getId()};
    });
    codingDemos.addObject().put("caseId",caseId).put("problemId",ids[0]).put("submissionId",ids[1]);
   }
  }
  var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  session.put("baseUrl","http://127.0.0.1:"+server.getAddress().getPort());
  var output=Path.of("../obo-harness/evidence/2026-11-01-integration-http");Files.createDirectories(output);
  var stop=new java.util.concurrent.CountDownLatch(1);
  server.createContext("/",exchange->{
   try{
    String endpoint=exchange.getRequestURI().getPath();
    if(endpoint.equals("/api/obo-preview-session")){respond(exchange,200,mapper.writeValueAsBytes(session));return;}
    String auth=exchange.getRequestHeaders().getFirst("Authorization");long userId=("Bearer "+authorToken).equals(auth)?author:("Bearer "+studentToken).equals(auth)?student:-1;
    if(userId<0){respond(exchange,401,"{}".getBytes(StandardCharsets.UTF_8));return;}
    if(endpoint.equals("/api/obo-preview-stop")){respond(exchange,200,"{}".getBytes(StandardCharsets.UTF_8));stop.countDown();return;}
    if(endpoint.equals("/api/profile/show")){respond(exchange,200,"{\"data\":{\"userInfo\":{}}}".getBytes(StandardCharsets.UTF_8));return;}
    byte[] body=exchange.getRequestBody().readNBytes(2*1048576+1);if(body.length>2*1048576){respond(exchange,413,new byte[0]);return;}
    var response=tx.execute(s->{try{
     org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(String.valueOf(userId),null,List.of()));
     var req=MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(exchange.getRequestMethod()),exchange.getRequestURI()).contentType("application/json").content(body);
     var result=mvc.perform(req).andReturn().getResponse();if(result.getStatus()>=400)s.setRollbackOnly();return result;
    }catch(Exception e){throw new RuntimeException(e);}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}});
    respond(exchange,response.getStatus(),response.getContentAsByteArray());
   }catch(Exception error){error.printStackTrace();respond(exchange,500,"{\"detail\":\"local test request failed\"}".getBytes(StandardCharsets.UTF_8));}
  });
  server.start();Files.writeString(output.resolve("session.json"),mapper.writeValueAsString(session));
  System.out.println("OBO_LOCAL_HTTP="+session.get("baseUrl").asText());
  try{stop.await(45,java.util.concurrent.TimeUnit.MINUTES);}finally{server.stop(0);session.put("expiresAt",0);Files.writeString(output.resolve("session.json"),mapper.writeValueAsString(session));}
 }
 static String token(String sub){return "e30."+Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\""+sub+"\"}").getBytes(StandardCharsets.UTF_8))+".local-preview";}
 static void respond(com.sun.net.httpserver.HttpExchange exchange,int status,byte[] bytes)throws java.io.IOException{exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}}
}
