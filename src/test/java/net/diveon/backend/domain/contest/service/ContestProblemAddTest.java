package net.diveon.backend.domain.contest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestProblem;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestProblemAlreadyExistsException;
import net.diveon.backend.global.exception.InvalidContestProblemPointsException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class ContestProblemAddTest {
    private final ContestRepository contests = mock(ContestRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    private final ContestProblemRepository contestProblems = mock(ContestProblemRepository.class);
    private final ProblemRepository problems = mock(ProblemRepository.class);
    private final ContestProblemNormalService service = new ContestProblemNormalService(contests, users, participants,
            contestProblems, mock(ContestSubmissionRepository.class), problems, mock(ProblemAccessService.class));

    private User user(long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @BeforeEach
    void fixture() {
        User host = user(7L);
        User other = user(8L);
        Contest contest = new Contest();
        ReflectionTestUtils.setField(contest, "id", 3L);
        ReflectionTestUtils.setField(contest, "createdBy", host);
        when(contests.findById(3L)).thenReturn(Optional.of(contest));
        when(users.existsById(7L)).thenReturn(true);
        when(participants.findByContestIdAndUserId(3L, 7L))
                .thenReturn(Optional.of(new ContestParticipant(contest, host, ContestParticipant.ContestRole.ADMIN)));
        Problem own = new Problem(host, "coding", "own", "Memory", "1", "private");
        ReflectionTestUtils.setField(own, "id", 110L);
        Problem others = new Problem(other, "coding", "others", "Memory", "1", "public");
        ReflectionTestUtils.setField(others, "id", 91L);
        when(problems.findById(110L)).thenReturn(Optional.of(own));
        when(problems.findById(91L)).thenReturn(Optional.of(others));
        when(contestProblems.findByContestIdAndProblemId(anyLong(), anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void addsOwnProblemWithDefaultPoints() {
        service.addProblemToContestServiceLogic(3L, 110L, 7L, null);
        ArgumentCaptor<ContestProblem> saved = ArgumentCaptor.forClass(ContestProblem.class);
        verify(contestProblems).saveAndFlush(saved.capture());
        assertEquals(100, saved.getValue().getPoints());
        assertEquals(110L, saved.getValue().getProblem().getId());
    }

    @Test
    void rejectsSomeoneElsesProblem() {
        assertThrows(ProblemNotFoundException.class, () -> service.addProblemToContestServiceLogic(3L, 91L, 7L, 100));
        verify(contestProblems, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateBeforeSaving() {
        when(contestProblems.findByContestIdAndProblemId(3L, 110L)).thenReturn(Optional.of(new ContestProblem()));
        assertThrows(ContestProblemAlreadyExistsException.class,
                () -> service.addProblemToContestServiceLogic(3L, 110L, 7L, 100));
        verify(contestProblems, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateBecomesConflict() {
        when(contestProblems.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("UK_CONTEST_PROBLEM_UNIQUE"));
        assertThrows(ContestProblemAlreadyExistsException.class,
                () -> service.addProblemToContestServiceLogic(3L, 110L, 7L, 100));
    }

    @Test
    void rejectsNegativePoints() {
        assertThrows(InvalidContestProblemPointsException.class,
                () -> service.addProblemToContestServiceLogic(3L, 110L, 7L, -1));
    }
}
