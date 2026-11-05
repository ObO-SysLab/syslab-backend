package net.diveon.backend.domain.contest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.entity.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class ContestSubmissionBoardTest {
    ContestProblemRepository problems = mock(ContestProblemRepository.class);
    ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    ContestSubmissionRepository submissions = mock(ContestSubmissionRepository.class);
    SolveSubmissionCodingRepository codingSubmissions = mock(SolveSubmissionCodingRepository.class);
    SolveResultRepository results = mock(SolveResultRepository.class);
    SolveResultCodingRepository codingResults = mock(SolveResultCodingRepository.class);
    ProblemAccessService access = mock(ProblemAccessService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneOffset.UTC);
    LocalDateTime now = LocalDateTime.now(clock);
    ContestSubmissionBoardService service = new ContestSubmissionBoardService(problems, participants, submissions,
            codingSubmissions, results, codingResults, access, clock);
    User user = new User(); Contest contest = new Contest();
    ContestParticipant participant;
    Problem problem; ContestProblem link; ContestSubmission row;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(user, "id", 7L); ReflectionTestUtils.setField(user, "nickname", "me");
        ReflectionTestUtils.setField(contest, "id", 1L); contest.updateTime(now.minusHours(1), now.plusHours(1));
        problem = new Problem(user, "practice", "fixture", "OS", "easy", "public"); ReflectionTestUtils.setField(problem, "id", 2L);
        link = new ContestProblem(contest, problem, 100); ReflectionTestUtils.setField(link, "id", 9L);
        participant = new ContestParticipant(contest, user, ContestParticipant.ContestRole.PARTICIPANT); participant.addScore(100, now);
        row = new ContestSubmission(contest, link, user); ReflectionTestUtils.setField(row, "id", 17L); row.updateResult(true);
        when(problems.findByIdAndContestId(9L, 1L)).thenReturn(Optional.of(link));
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.of(participant));
        when(submissions.findByContestIdAndContestProblemIdAndUserId(eq(1L), eq(9L), eq(7L), any()))
            .thenAnswer(i -> new PageImpl<>(List.of(row), i.getArgument(3), 1));
    }
    @ParameterizedTest @ValueSource(strings = {"objective", "practice"})
    void immediateResultsPersistAcrossFreshBoardReads(String type) {
        ReflectionTestUtils.setField(problem, "type", type);
        for (boolean verdict : new boolean[]{true, false}) {
            row.updateResult(verdict);
            for (int read = 0; read < 2; read++) {
                var board = service.getBoard(7L, 1L, 9L, 1, 50); var item = board.submissions().getFirst();
                assertEquals(17L, item.submissionId()); assertNull(item.solveSubmissionId());
                assertEquals("COMPLETED", item.submissionStatus()); assertEquals(verdict, item.isCorrect());
                assertEquals(100, board.currentTotalScore()); assertEquals(1, board.total());
            }
        }
        verifyNoInteractions(codingSubmissions, results, codingResults);
    }
    SolveSubmission coding() {
        ReflectionTestUtils.setField(problem, "type", "coding");
        var solve = new SolveSubmission(problem, user); ReflectionTestUtils.setField(solve, "id", 92L);
        ReflectionTestUtils.setField(row, "solveSubmission", solve); row.updateStatus("PENDING"); row.updateResult(null);
        return solve;
    }
    @Test void codingUsesContestIdForRowAndLinkedSolveIdForDetails() {
        var solve = coding(); solve.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED); row.updateResultAndStatus(true, "COMPLETED");
        when(codingSubmissions.findById(92L)).thenReturn(Optional.of(new SolveSubmissionCoding(solve, "private source", "python")));
        var result = new SolveResult(solve, SolveResult.SolveResultState.CORRECT); ReflectionTestUtils.setField(result, "id", 52L);
        when(results.findBySubmissionId(92L)).thenReturn(Optional.of(result));
        when(codingResults.findById(52L)).thenReturn(Optional.of(new SolveResultCoding(result, (short)100, 16, 42, 12)));
        var item = service.getBoard(7L, 1L, 9L, 1, 50).submissions().getFirst();
        assertEquals(17L, item.submissionId()); assertEquals(92L, item.solveSubmissionId());
        assertEquals("python", item.language()); assertEquals(42, item.runtime()); assertEquals(16, item.memoryUsage());
        assertFalse(item.toString().contains("private source")); verify(results, never()).findBySubmissionId(17L);
    }
    @Test void linkedJudgingStateIsVisibleBeforeContestCompletion() {
        coding().setSubmissionState(SolveSubmission.SubmissionState.JUDGING);
        var item = service.getBoard(7L, 1L, 9L, 1, 50).submissions().getFirst();
        assertEquals("JUDGING", item.submissionStatus()); assertNull(item.isCorrect()); verifyNoInteractions(results);
    }
    @Test void failedCodingIsTerminalWithoutFakeWrongAnswerOrMetrics() {
        coding().setSubmissionState(SolveSubmission.SubmissionState.FAILED); row.updateStatus("FAILED");
        var item = service.getBoard(7L, 1L, 9L, 1, 50).submissions().getFirst();
        assertEquals("FAILED", item.submissionStatus()); assertNull(item.isCorrect()); assertNull(item.runtime()); verifyNoInteractions(results);
    }
    @Test void mismatchedCodingLinkIsRejectedBeforeReadingSourceOrMetrics() {
        var solve = coding(); var other = new User(); ReflectionTestUtils.setField(other, "id", 8L);
        ReflectionTestUtils.setField(solve, "user", other);
        assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50));
        verifyNoInteractions(codingSubmissions, results, codingResults);
    }
    @Test void mismatchedContestProblemIsRejectedBeforeReadingSubmissions() {
        when(problems.findByIdAndContestId(9L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50));
        verifyNoInteractions(submissions);
    }
    @Test void nonParticipantIncludingAuthorCannotReadBoard() {
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50)); verifyNoInteractions(submissions);
    }
    @Test void bannedParticipantCannotReadBoard() {
        participant.ban(); assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50)); verifyNoInteractions(submissions);
    }
    @Test void beforeStartIsDeniedEvenForPublicProblem() {
        contest.updateTime(now.plusMinutes(1), now.plusHours(1));
        assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50)); verifyNoInteractions(submissions);
    }
    @Test void endedContestStillAllowsParticipantHistory() {
        contest.updateTime(now.minusHours(2), now.minusHours(1));
        assertEquals(1, service.getBoard(7L, 1L, 9L, 1, 50).submissions().size());
    }
    @Test void revokedProblemAccessDeniesBeforeRows() {
        doThrow(new ProblemNotFoundException()).when(access).requireReadInContest(7L, 2L, 1L);
        assertThrows(ProblemNotFoundException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50)); verifyNoInteractions(submissions);
    }
    @Test void ownerIsAlwaysPartOfRepositoryQueryAndSortingIsStable() {
        service.getBoard(7L, 1L, 9L, 2, 25);
        verify(submissions).findByContestIdAndContestProblemIdAndUserId(eq(1L), eq(9L), eq(7L), argThat(p ->
            p.getPageNumber() == 1 && p.getPageSize() == 25 && p.getSort().getOrderFor("id").isDescending()));
    }
    @Test void accidentalForeignRowFailsClosed() {
        var other = new User(); ReflectionTestUtils.setField(other, "id", 8L); ReflectionTestUtils.setField(row, "user", other);
        assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50));
    }
    @Test void invalidPaginationCannotReadDatabase() {
        for (int[] limits : new int[][]{{0, 50}, {1, 0}, {1, 101}, {1000001, 1}})
            assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, limits[0], limits[1]));
        verifyNoInteractions(problems, submissions);
    }
    @Test void missingVerdictIsNotConvertedToWrongAnswer() {
        row.updateResult(null); assertNull(service.getBoard(7L, 1L, 9L, 1, 50).submissions().getFirst().isCorrect());
    }
    @Test void unknownStateIsUnavailableRatherThanPermanentPending() {
        row.updateStatus("UNKNOWN"); assertThrows(ResponseStatusException.class, () -> service.getBoard(7L, 1L, 9L, 1, 50));
    }
}
