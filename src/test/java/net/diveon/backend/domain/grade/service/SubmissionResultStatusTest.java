package net.diveon.backend.domain.grade.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.grade.entity.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

class SubmissionResultStatusTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    final SolveResultRepository results = mock(SolveResultRepository.class);
    final SolveSubmissionCodingRepository coding = mock(SolveSubmissionCodingRepository.class);
    final SolveSubmissionObjectiveRepository objectives = mock(SolveSubmissionObjectiveRepository.class);
    final SolveResultCodingRepository metrics = mock(SolveResultCodingRepository.class);
    final ProblemAccessService access = mock(ProblemAccessService.class);
    final GradeBoardService board = new GradeBoardService(submissions, coding, results, metrics,
            mock(ProblemRepository.class), access, mock(SubmissionOboPresence.class));
    final SubmissionResultService detail = new SubmissionResultService(submissions, results, objectives, coding, metrics);
    final SubmissionStatusService polling = new SubmissionStatusService(submissions, results);
    SolveSubmission submission;

    void fixture(String type, SolveResult.SolveResultState verdict) {
        var user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        var problem = new Problem(user, type, "fixture", "kernel", "2", "private");
        ReflectionTestUtils.setField(problem, "id", 22L);
        submission = new SolveSubmission(problem, user); ReflectionTestUtils.setField(submission, "id", 158L);
        submission.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);
        var result = new SolveResult(submission, verdict); ReflectionTestUtils.setField(result, "id", 42L);
        when(submissions.findById(158L)).thenReturn(Optional.of(submission));
        when(submissions.findTerminalByProbId(eq(22L), any())).thenReturn(new PageImpl<>(List.of(submission)));
        when(submissions.findCompletedByProbId(eq(22L), any())).thenReturn(new PageImpl<>(List.of(submission)));
        when(submissions.countCompletedByProbId(22L)).thenReturn(1L);
        when(access.requireRead(7L, 22L)).thenReturn(problem);
        when(results.findBySubmissionId(158L)).thenReturn(Optional.of(result));
        when(coding.findById(158L)).thenReturn(Optional.of(new SolveSubmissionCoding(submission, "print(1)", "python")));
        when(objectives.findById(158L)).thenReturn(Optional.of(new SolveSubmissionObjective(submission, List.of(1))));
        when(metrics.findById(42L)).thenReturn(Optional.of(new SolveResultCoding(result, (short) 42, 22, 33, 8)));
    }

    JsonNode row(String filter) {
        return mapper.valueToTree(board.getBoard(7L, 22L, 1, 20, filter, "latest")).path("submissions");
    }

    @ParameterizedTest
    @CsvSource({"coding,CORRECT", "coding,WRONG", "coding,ERROR", "objective,CORRECT", "objective,WRONG",
            "objective,ERROR", "practice,CORRECT", "practice,WRONG", "practice,ERROR"})
    void savedVerdictSurvivesBoardDetailAndPollingSerialization(String type, SolveResult.SolveResultState verdict) {
        fixture(type, verdict);
        var boardJson = row("all").get(0);
        var detailJson = mapper.valueToTree(detail.getResult(7L, 158L));
        var statusJson = mapper.valueToTree(polling.getStatus(7L, 158L));
        for (var response : List.of(boardJson, detailJson, statusJson))
            assertEquals(verdict.name(), response.path("resultStatus").textValue());
        assertEquals(verdict == SolveResult.SolveResultState.CORRECT, boardJson.path("isCorrect").booleanValue());
        assertEquals(verdict == SolveResult.SolveResultState.CORRECT, detailJson.path("isCorrect").booleanValue());
        assertEquals("COMPLETED", statusJson.path("submissionStatus").textValue());
        assertEquals(100, statusJson.path("progress").intValue());
        if ("coding".equals(type)) {
            assertEquals(42, detailJson.path("score").intValue());
            assertEquals(33, detailJson.path("runtime").intValue());
            assertEquals("print(1)", detailJson.path("code").textValue());
        }
        verify(results, times(3)).findBySubmissionId(158L); // No extra verdict query per board/detail row.
    }

    @ParameterizedTest @EnumSource(value = SolveSubmission.SubmissionState.class, names = {"PENDING", "JUDGING", "FAILED"})
    void unfinishedOrFailedLifecycleNeverBecomesAResult(SolveSubmission.SubmissionState state) {
        fixture("coding", SolveResult.SolveResultState.ERROR);
        submission.setSubmissionState(state);
        var status = mapper.valueToTree(polling.getStatus(7L, 158L));
        assertEquals(state.name(), status.path("submissionStatus").textValue());
        assertTrue(status.has("resultStatus")); assertTrue(status.get("resultStatus").isNull());
        verifyNoInteractions(results);
        assertThrows(SubmissionNotCompletedException.class, () -> detail.getResult(7L, 158L));
        verifyNoInteractions(results);
        if (state == SolveSubmission.SubmissionState.FAILED) assertTrue(row("all").get(0).get("resultStatus").isNull());
    }

    @Test void missingCompletedResultIsExplicitNullRatherThanWrong() {
        fixture("coding", SolveResult.SolveResultState.ERROR);
        when(results.findBySubmissionId(158L)).thenReturn(Optional.empty());
        assertTrue(row("all").get(0).get("resultStatus").isNull());
        assertTrue(mapper.valueToTree(polling.getStatus(7L, 158L)).get("resultStatus").isNull());
        assertThrows(SubmissionNotFoundException.class, () -> detail.getResult(7L, 158L));
    }

    @Test void wrongFilterRetainsExistingNonCorrectCompatibility() {
        fixture("coding", SolveResult.SolveResultState.ERROR);
        assertEquals("ERROR", row("wrong").get(0).path("resultStatus").textValue());
        assertTrue(row("correct").isEmpty());
    }

    @Test void otherUserCannotReadResultOrPollingVerdict() {
        fixture("coding", SolveResult.SolveResultState.ERROR);
        assertThrows(SubmissionAccessDeniedException.class, () -> detail.getResult(8L, 158L));
        assertThrows(SubmissionAccessDeniedException.class, () -> polling.getStatus(8L, 158L));
        verifyNoInteractions(results, coding, metrics);
    }
}
