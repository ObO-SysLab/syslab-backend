package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.group.repository.*;
import net.diveon.backend.domain.grade.service.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.vm.service.*;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import net.diveon.backend.domain.vm.dto.request.*;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Real shared policy and real entry services; only persistence/external execution are mocked. */
class ProblemAccessBoundaryTest {
    ProblemRepository problems = mock(ProblemRepository.class);
    UserRepository users = mock(UserRepository.class);
    ProblemPracticeRepository practices = mock(ProblemPracticeRepository.class);
    ProblemCodingRepository codings = mock(ProblemCodingRepository.class);
    ProblemObjectiveRepository objectives = mock(ProblemObjectiveRepository.class);
    SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    SolveSubmissionCodingRepository codingSubmissions = mock(SolveSubmissionCodingRepository.class);
    SolveResultRepository results = mock(SolveResultRepository.class);
    SolveResultCodingRepository codingResults = mock(SolveResultCodingRepository.class);
    ProblemCommentRepository comments = mock(ProblemCommentRepository.class);
    DockerService docker = mock(DockerService.class);
    VmSessionRepository sessions = mock(VmSessionRepository.class);
    ProblemAccessService access = new ProblemAccessService(problems, mock(GroupProblemRepository.class),
            mock(GroupUserRepository.class), mock(ContestProblemRepository.class), mock(ContestParticipantRepository.class));

    @BeforeEach void privateProblem() {
        User owner = new User(); ReflectionTestUtils.setField(owner, "id", 1L);
        Problem problem = new Problem(owner, "practice", "hidden", "OS", "easy", "private");
        ReflectionTestUtils.setField(problem, "id", 9L);
        when(problems.findById(9L)).thenReturn(Optional.of(problem));
        when(problems.findForRevision(9L)).thenReturn(Optional.of(problem));
        when(users.findById(7L)).thenReturn(Optional.of(new User()));
    }
    @Test void detailDeniesBeforeLoadingSubtypeOrObo() {
        var obo = mock(OboStepRepository.class);
        var service = new ProblemDetailService(users, problems, objectives, practices, codings, obo, access, mock(OboDisclosureService.class));
        assertThrows(ProblemNotFoundException.class, () -> service.detailProblem(7, 9));
        verifyNoInteractions(practices, objectives, codings, obo);
    }
    @Test void generalSubmissionDeniesBeforeSavingCountingOrSubtypeAccess() {
        var objectiveSubmissions = mock(SolveSubmissionObjectiveRepository.class);
        var practiceSubmissions = mock(SolveSubmissionPracticeRepository.class);
        var service = new SubmissionGradeStarterService(results, submissions, objectiveSubmissions, codingSubmissions,
                practiceSubmissions, users, problems, codings, objectives, practices, access, mock(ProblemRevisionService.class));
        var request = new SubmissionGradeReqeust(); ReflectionTestUtils.setField(request, "prodId", 9L);
        assertThrows(ProblemNotFoundException.class, () -> service.submissionGradeStart(7, request));
        verifyNoInteractions(submissions, objectiveSubmissions, codingSubmissions, practiceSubmissions, practices);
        verify(problems, never()).incrementSubmittedCount(9L);
    }
    @Test void boardDeniesBeforeReadingOtherSubmissions() {
        var service = new GradeBoardService(submissions, codingSubmissions, results, codingResults, problems, access,
                new SubmissionOboPresence(codings, objectives));
        assertThrows(ProblemNotFoundException.class, () -> service.getBoard(7L, 9L, 1, 20, "all", "latest"));
        verifyNoInteractions(submissions, codingSubmissions, results, codingResults);
    }
    @Test void vmCreateResetAndStatusDenyBeforeDockerOrSessionMutation() {
        var store = mock(VmSessionStore.class);
        var service = new VmService(sessions, practices, docker, access, store);
        var create = new VmCreateRequest(); create.setProbId(9L);
        var reset = new VmStopRequest(); ReflectionTestUtils.setField(reset, "probId", 9L);
        assertThrows(ProblemNotFoundException.class, () -> service.createVm(7L, create));
        assertThrows(ProblemNotFoundException.class, () -> service.resetVm(7L, reset));
        assertThrows(ProblemNotFoundException.class, () -> service.getStatus(7L, 9L));
        verifyNoInteractions(docker, sessions, practices, store);
    }
    @Test void oboAndRankingCannotBypassDetailAccess() {
        var obo = new ProblemOboService(problems, objectives, codings, access, mock(OboDisclosureService.class), submissions, mock(ContestSubmissionRepository.class), mock(net.diveon.backend.domain.grade.repository.SolveResultRepository.class));
        var ranking = new ProblemRankingService(submissions, codingSubmissions, problems, access);
        assertThrows(ProblemNotFoundException.class, () -> obo.getOboJson(7L, 9L));
        assertThrows(ProblemNotFoundException.class, () -> ranking.getRanking(7, 9, 1));
        verifyNoInteractions(objectives, codings, submissions, codingSubmissions);
    }
    @Test void commentsCannotBypassDetailAccessWithAnotherProblemId() {
        var service = new ProblemCommentService(comments, problems, users, access);
        assertThrows(ProblemNotFoundException.class, () -> service.getComments(7, 9, 1, 20));
        assertThrows(ProblemNotFoundException.class, () -> service.getComment(7, 9, 33));
        verifyNoInteractions(comments);
    }
}
