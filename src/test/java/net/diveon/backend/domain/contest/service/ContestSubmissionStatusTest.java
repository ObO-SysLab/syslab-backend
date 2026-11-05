package net.diveon.backend.domain.contest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.grade.entity.*;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ContestSubmissionStatusTest {
    final ObjectMapper mapper = new ObjectMapper();
    final ContestSubmissionRepository submissions = mock(ContestSubmissionRepository.class);
    final SolveResultRepository results = mock(SolveResultRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final ContestSubmissionStatusService service = new ContestSubmissionStatusService(submissions,
            mock(StringRedisTemplate.class), jdbc, results);
    ContestSubmission row;
    SolveSubmission solve;

    void fixture(String type, boolean linked) {
        var user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        var contest = new Contest(); ReflectionTestUtils.setField(contest, "id", 1L);
        var problem = new Problem(user, type, "fixture", "kernel", "2", "private");
        var link = new ContestProblem(contest, problem, 100);
        solve = new SolveSubmission(problem, user); ReflectionTestUtils.setField(solve, "id", 158L);
        solve.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);
        row = new ContestSubmission(contest, link, user, linked ? solve : null, "COMPLETED");
        row.updateResult(false);
        when(submissions.findById(17L)).thenReturn(Optional.of(row));
        when(jdbc.queryForObject(startsWith("SELECT score"), eq(Integer.class), eq(1L), eq(7L))).thenReturn(45);
        when(jdbc.queryForObject(eq(ContestRankQuery.SQL), eq(Integer.class), eq(1L), eq(7L))).thenReturn(3);
    }

    @ParameterizedTest @EnumSource(SolveResult.SolveResultState.class)
    void pollingUsesOriginalCodingVerdictAndPreservesScoreRankAndBoolean(SolveResult.SolveResultState verdict) {
        fixture("coding", true);
        when(results.findBySubmissionId(158L)).thenReturn(Optional.of(new SolveResult(solve, verdict)));
        var json = mapper.valueToTree(service.getStatus(1L, 17L, 7L));
        assertEquals(verdict.name(), json.path("resultStatus").textValue());
        assertFalse(json.path("isCorrect").booleanValue());
        assertEquals(45, json.path("score").intValue()); assertEquals(3, json.path("rank").intValue());
        assertEquals("COMPLETED", json.path("submissionStatus").textValue());
        verify(results).findBySubmissionId(158L); verify(results, never()).findBySubmissionId(17L);
    }

    @ParameterizedTest @CsvSource({"objective,true,CORRECT", "objective,false,WRONG", "practice,true,CORRECT", "practice,false,WRONG"})
    void immediateContestVerdictsRemainAvailableWithoutSolveRows(String type, boolean correct, String expected) {
        fixture(type, false); row.updateResult(correct);
        assertEquals(expected, mapper.valueToTree(service.getStatus(1L, 17L, 7L)).path("resultStatus").textValue());
        verifyNoInteractions(results);
    }

    @ParameterizedTest @EnumSource(value = SolveSubmission.SubmissionState.class, names = {"PENDING", "JUDGING", "FAILED"})
    void nonCompletedStatusHasExplicitNullAndDoesNotReadResultOrScore(SolveSubmission.SubmissionState state) {
        fixture("coding", true); solve.setSubmissionState(state); row.updateStatus(state.name());
        var json = mapper.valueToTree(service.getStatus(1L, 17L, 7L));
        assertEquals(state.name(), json.path("submissionStatus").textValue());
        assertTrue(json.has("resultStatus")); assertTrue(json.get("resultStatus").isNull());
        verifyNoInteractions(results, jdbc);
    }

    @Test void missingOrUnfinishedLinkedResultNeverUsesContestFalseAsWrong() {
        fixture("coding", true);
        assertTrue(mapper.valueToTree(service.getStatus(1L, 17L, 7L)).get("resultStatus").isNull());
        solve.setSubmissionState(SolveSubmission.SubmissionState.FAILED);
        assertTrue(mapper.valueToTree(service.getStatus(1L, 17L, 7L)).get("resultStatus").isNull());
        verify(results, times(1)).findBySubmissionId(158L);
    }

    @Test void unlinkedCodingAndUnknownImmediateVerdictRemainNull() {
        fixture("coding", false);
        assertTrue(mapper.valueToTree(service.getStatus(1L, 17L, 7L)).get("resultStatus").isNull());
        fixture("objective", false); row.updateResult(null);
        assertTrue(mapper.valueToTree(service.getStatus(1L, 17L, 7L)).get("resultStatus").isNull());
        verifyNoInteractions(results);
    }

    @Test void foreignUserAndContestAreDeniedBeforeVerdictLookup() {
        fixture("coding", true);
        assertThrows(SubmissionAccessDeniedException.class, () -> service.getStatus(1L, 17L, 8L));
        assertThrows(ContestNotFoundException.class, () -> service.getStatus(2L, 17L, 7L));
        verifyNoInteractions(results, jdbc);
    }
}
