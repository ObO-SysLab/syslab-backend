package net.diveon.backend.domain.grade.service;

import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.diveon.backend.domain.grade.entity.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.user.service.UserTierService;

class SubmissionGradeAsyncServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"objective", "practice"})
    void firstCorrectUsesAtomicCounterWithoutMutatingLoadedProblem(String type) throws Exception {
        var users = mock(UserRepository.class);
        var problems = mock(ProblemRepository.class);
        var objectives = mock(ProblemObjectiveRepository.class);
        var practices = mock(ProblemPracticeRepository.class);
        var submissions = mock(SolveSubmissionRepository.class);
        var objectiveSubmissions = mock(SolveSubmissionObjectiveRepository.class);
        var practiceSubmissions = mock(SolveSubmissionPracticeRepository.class);
        var results = mock(SolveResultRepository.class);
        var tiers = mock(UserTierService.class);
        var service = new SubmissionGradeAsyncService(users, problems, objectives,
                mock(ProblemCodingRepository.class), practices, submissions, objectiveSubmissions,
                mock(SolveSubmissionCodingRepository.class), practiceSubmissions, results,
                mock(CodingGradeQueueService.class), tiers);
        User user = new User();
        Problem problem = spy(new Problem(user, type, "test", "OS", "easy", "public"));
        SolveSubmission submission = new SolveSubmission(problem, user);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(problems.findById(2L)).thenReturn(Optional.of(problem));
        when(submissions.findById(101L)).thenReturn(Optional.of(submission));
        if ("objective".equals(type)) {
            var objective = mock(ProblemObjective.class);
            when(objective.getAnswer()).thenReturn(List.of(1));
            when(objectives.findById(2L)).thenReturn(Optional.of(objective));
            when(objectiveSubmissions.findById(101L)).thenReturn(
                    Optional.of(new SolveSubmissionObjective(submission, List.of(1))));
            service.gradeProblemObjective(2L, 7L, 101L);
        } else {
            var practice = mock(ProblemPractice.class);
            when(practice.getFlagHash()).thenReturn(HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest("flag".getBytes(StandardCharsets.UTF_8))));
            when(practices.findById(2L)).thenReturn(Optional.of(practice));
            when(practiceSubmissions.findById(101L)).thenReturn(
                    Optional.of(new SolveSubmissionPractice(submission, "flag")));
            service.gradeProblemPractice(2L, 7L, 101L);
        }
        verify(problems).recordSolver(2L, 7L);
        verify(problem, never()).incrementSolvedCount();
        verify(problems, never()).save(any());
        verify(tiers).refreshTierScore(7L);
    }
}
