package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemDetailCodingResponse;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.Test;

class ProblemOboDisclosureTest {
    final ProblemAccessService access = mock(ProblemAccessService.class);
    final ContestProblemRepository links = mock(ContestProblemRepository.class);
    final ContestParticipantRepository members = mock(ContestParticipantRepository.class);
    final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    final ProblemObjectiveRepository objectives = mock(ProblemObjectiveRepository.class);
    final ProblemCodingRepository codings = mock(ProblemCodingRepository.class);
    final LocalDateTime now = LocalDateTime.of(2026,10,31,12,0);
    final OboDisclosureService disclosure = new OboDisclosureService(links, members, submissions,
        Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    final ContestSubmissionRepository contests=mock(ContestSubmissionRepository.class);
    final net.diveon.backend.domain.grade.repository.SolveResultRepository results = mock(net.diveon.backend.domain.grade.repository.SolveResultRepository.class);
    final ProblemOboService service = new ProblemOboService(mock(ProblemRepository.class), objectives, codings, access, disclosure, submissions, contests, results);

    Problem problem(String type) {
        var problem = mock(Problem.class); var author = mock(User.class);
        when(author.getId()).thenReturn(1L); when(problem.getAuthor()).thenReturn(author);
        when(problem.getType()).thenReturn(type);
        when(access.requireRead(anyLong(), eq(9L))).thenReturn(problem);
        return problem;
    }
    @Test void readableProblemAloneCannotExposeDocument() {
        problem("objective");
        assertThrows(ProblemNotFoundException.class, () -> service.getOboJson(7L, 9L));
        verifyNoInteractions(objectives, codings);
    }
    @Test void completedStudyStillCannotReadDuringContest() {
        problem("coding");
        when(submissions.countDisclosureEligible(7L,9L,now)).thenReturn(1L);
        var contest = mock(Contest.class); when(contest.getId()).thenReturn(3L);
        when(contest.getStartTime()).thenReturn(now.minusMinutes(1));
        when(contest.getEndTime()).thenReturn(now.plusMinutes(1));
        var link = mock(ContestProblem.class); when(link.getContest()).thenReturn(contest);
        when(links.findAllWithContestByProblemId(9L)).thenReturn(List.of(link));
        when(members.findByContestIdAndUserId(3L,7L)).thenReturn(Optional.of(new ContestParticipant()));
        assertThrows(ProblemNotFoundException.class, () -> service.getOboJson(7L,9L));
        verifyNoInteractions(objectives, codings);
    }
    @Test void completedStudyReadsLegacyDocumentWithoutChangingIt() {
        problem("objective");
        when(submissions.countDisclosureEligible(7L,9L,now)).thenReturn(1L);
        var objective = mock(ProblemObjective.class);
        var document = JsonNodeFactory.instance.objectNode().put("mode","per_choice");
        when(objective.getOboJsonData()).thenReturn(document);
        when(objectives.findById(9L)).thenReturn(Optional.of(objective));
        assertEquals(document,service.getOboJson(7L,9L));
    }
    @Test void authorEditPathDoesNotGrantLearnerAccess() {
        problem("objective");
        assertThrows(ProblemNotFoundException.class, () -> service.getAuthorOboJson(7L,9L));
        var objective = mock(ProblemObjective.class);
        when(objectives.findById(9L)).thenReturn(Optional.of(objective));
        service.getAuthorOboJson(1L,9L);
        assertThrows(ProblemNotFoundException.class, () -> service.getOboJson(1L,9L));
    }
    @Test void learnerDetailNeverReturnsHiddenTestcasesEvenForAuthor() {
        var problem = problem("coding");
        when(problem.getCreatedAt()).thenReturn(now); when(problem.getUpatedAt()).thenReturn(now);
        var coding = mock(ProblemCoding.class);
        when(coding.getTestcases()).thenReturn(List.of(
            new ForDtoTestCase(1,"public","public",true),
            new ForDtoTestCase(2,"hidden","secret",false),
            new ForDtoTestCase(3,"unclassified","secret",null)));
        for(boolean owner : List.of(false,true)) {
            var response = ProblemDetailCodingResponse.of(problem,coding,owner);
            assertEquals(1,response.getTestcases().size());
            assertEquals("public",response.getTestcases().getFirst().getInput());
        }
    }
    @Test void submissionReadsFrozenOboWithoutLoadingCurrentProblemDocument() {
        var problem=problem("objective");when(problem.getId()).thenReturn(9L);
        var user=mock(User.class);when(user.getId()).thenReturn(7L);
        var submission=mock(net.diveon.backend.domain.grade.entity.SolveSubmission.class);
        when(submission.getProblem()).thenReturn(problem);when(submission.getUser()).thenReturn(user);
        when(submission.getSubmissionState()).thenReturn(net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED);
        var snapshot=JsonNodeFactory.instance.objectNode();snapshot.putObject("obo").put("mode","single").put("marker","original");
        snapshot.putArray("testcases").add("hidden");snapshot.putArray("answer").add(2);
        when(submission.getProblemRevision()).thenReturn(new ProblemRevision("9:old",9L,snapshot));
        snapshot.set("testcases",new com.fasterxml.jackson.databind.ObjectMapper().createArrayNode()
            .add(JsonNodeFactory.instance.objectNode().put("isSample",true))
            .add(JsonNodeFactory.instance.objectNode().put("isSample",false)));
        when(submission.getProblemRevision()).thenReturn(new ProblemRevision("9:old",9L,snapshot));
        when(submission.getPublicOboRecords()).thenReturn(JsonNodeFactory.instance.arrayNode()
            .add(JsonNodeFactory.instance.objectNode().put("caseIndex",1).put("message","public").put("internalDebug","must not leave server"))
            .add(JsonNodeFactory.instance.objectNode().put("caseIndex",2).put("message","hidden secret"))
            .add(JsonNodeFactory.instance.objectNode().put("caseIndex",99).put("message","invalid index")));
        when(submissions.findById(11L)).thenReturn(Optional.of(submission));
        when(submissions.countDisclosureEligible(7L,9L,now)).thenReturn(1L);
        var response=service.getSubmissionObo(7L,11L);
        assertEquals("9:old",response.revisionId());assertFalse(response.legacyRevision());
        assertEquals("original",response.obo().path("marker").asText());
        assertFalse(response.obo().has("testcases"));assertFalse(response.obo().has("answer"));
        assertEquals(1,response.publicCases().size());assertFalse(response.publicCases().toString().contains("secret"));
        assertFalse(response.publicCases().get(0).has("internalDebug"));
        verifyNoInteractions(objectives,codings);
        assertThrows(ProblemNotFoundException.class,()->service.getSubmissionObo(8L,11L));
    }
    @Test void objectiveContestUsesOwnFrozenRevisionAndWaitsUntilEnd() {
        var problem=problem("objective");when(problem.getId()).thenReturn(9L);
        var contest=mock(Contest.class);when(contest.getId()).thenReturn(3L);
        when(contest.getStartTime()).thenReturn(now.minusHours(1));when(contest.getEndTime()).thenReturn(now.plusMinutes(1));
        var link=mock(ContestProblem.class);when(link.getContest()).thenReturn(contest);when(link.getProblem()).thenReturn(problem);
        var user=mock(User.class);when(user.getId()).thenReturn(7L);
        var submission=new ContestSubmission(contest,link,user,"COMPLETED");submission.updateResult(true);
        var snapshot=JsonNodeFactory.instance.objectNode();snapshot.putObject("obo").put("mode","single").put("marker","original contest");
        submission.bindRevision(new ProblemRevision("9:contest",9L,snapshot),JsonNodeFactory.instance.arrayNode().add(2));
        when(contests.findById(42L)).thenReturn(Optional.of(submission));
        when(access.requireReadInContest(7L,9L,3L)).thenReturn(problem);
        when(submissions.countCompletedContestForDisclosure(7L,9L,now)).thenReturn(1L);
        var lockedResponse=service.getContestSubmissionObo(7L,42L);assertNull(lockedResponse.resultStatus());
        var locked=lockedResponse.explanation();
        assertEquals("contest",locked.lockedReason());assertNull(locked.obo());assertNull(locked.publicCases());
        when(contest.getEndTime()).thenReturn(now);
        var released=service.getContestSubmissionObo(7L,42L);
        assertEquals("CORRECT",released.resultStatus());
        assertEquals("9:contest",released.explanation().revisionId());assertEquals(2,released.selectedAnswers().get(0).asInt());
        verifyNoInteractions(objectives,codings);
        assertThrows(ProblemNotFoundException.class,()->service.getContestSubmissionObo(8L,42L));
    }
    @Test void contestOboUsesCodingErrorAndPreservesOwnerAndEndTimeBoundary() {
        var problem=problem("coding");when(problem.getId()).thenReturn(9L);
        var user=mock(User.class);when(user.getId()).thenReturn(7L);
        var contest=mock(Contest.class);when(contest.getId()).thenReturn(3L);
        when(contest.getStartTime()).thenReturn(now.minusHours(1));when(contest.getEndTime()).thenReturn(now);
        var link=mock(ContestProblem.class);when(link.getProblem()).thenReturn(problem);when(link.getContest()).thenReturn(contest);
        var solve=new net.diveon.backend.domain.grade.entity.SolveSubmission(problem,user);
        var snapshot=JsonNodeFactory.instance.objectNode();snapshot.putObject("obo").put("mode","single");
        solve.bindRevision(new ProblemRevision("9:coding",9L,snapshot));
        org.springframework.test.util.ReflectionTestUtils.setField(solve,"id",11L);
        solve.setSubmissionState(net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED);
        var row=new ContestSubmission(contest,link,user,solve,"COMPLETED");row.updateResult(false);
        when(contests.findById(42L)).thenReturn(Optional.of(row));
        when(contests.findBySolveSubmissionId(11L)).thenReturn(Optional.of(row));
        when(submissions.findById(11L)).thenReturn(Optional.of(solve));
        when(submissions.countDisclosureEligible(7L,9L,now)).thenReturn(1L);
        when(access.requireReadInContest(7L,9L,3L)).thenReturn(problem);
        when(results.findBySubmissionId(11L)).thenReturn(Optional.of(new net.diveon.backend.domain.grade.entity.SolveResult(
            solve,net.diveon.backend.domain.grade.entity.SolveResult.SolveResultState.ERROR)));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
            .valueToTree(service.getContestSubmissionObo(7L,42L));
        assertEquals("ERROR",json.path("resultStatus").textValue());assertFalse(json.path("isCorrect").booleanValue());
        verify(results,times(1)).findBySubmissionId(11L);
        when(contest.getEndTime()).thenReturn(now.plusMinutes(1));
        var locked=service.getContestSubmissionObo(7L,42L);
        assertNull(locked.resultStatus());assertEquals("contest",locked.explanation().lockedReason());
        assertNull(locked.explanation().obo());
        assertThrows(ProblemNotFoundException.class,()->service.getContestSubmissionObo(8L,42L));
        verifyNoMoreInteractions(results);
    }
    @Test void projectionDropsGradingOnlyInstancesAndKeepsOnlyBoundExplanations() throws Exception {
        var raw=new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
            {"schemaVersion":2,"mode":"simulation","simulation":{
             "instances":[{"id":"explanation"},{"id":"gradingOnly","input":"private preset"}],
             "question":{"judgeProfile":"obo-v1","gradingInstanceId":"gradingOnly"},
             "explanations":{"common":["explanation"],"choices":{}}}}
            """);
        var projected=ProblemOboService.projectExplanation(raw);
        assertEquals(1,projected.path("simulation").path("instances").size());
        assertFalse(projected.toString().contains("private preset"));
        assertEquals(2,raw.path("simulation").path("instances").size());
    }
}
