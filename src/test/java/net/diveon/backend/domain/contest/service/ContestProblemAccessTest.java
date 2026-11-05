package net.diveon.backend.domain.contest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ContestProblemAccessTest {
    @Test void listingHidesDeniedProblemsAndKeepsAllowedProblemsInRequestedContest() {
        var contests = mock(ContestRepository.class);
        var users = mock(UserRepository.class);
        var participants = mock(ContestParticipantRepository.class);
        var links = mock(ContestProblemRepository.class);
        var submissions = mock(ContestSubmissionRepository.class);
        var access = mock(ProblemAccessService.class);
        var service = new ContestProblemNormalService(contests, users, participants, links, submissions,
                mock(ProblemRepository.class), access);
        when(contests.existsById(1L)).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(new User()));
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.of(new ContestParticipant()));
        var problem = new Problem(new User(), "practice", "fixture", "OS", "easy", "contest");
        ReflectionTestUtils.setField(problem, "id", 9L);
        var link = new ContestProblem(new Contest(), problem, 100);
        ReflectionTestUtils.setField(link, "id", 33L);
        when(links.findAllWithProblemByContestId(1L)).thenReturn(List.of(link));
        when(access.requireReadInContest(7L, 9L, 1L)).thenThrow(new ProblemNotFoundException());
        assertTrue(service.getContestProblems(1L, 7L).getProblems().isEmpty());
        doReturn(problem).when(access).requireReadInContest(7L, 9L, 1L);
        assertEquals(1, service.getContestProblems(1L, 7L).getProblems().size());
    }
}
