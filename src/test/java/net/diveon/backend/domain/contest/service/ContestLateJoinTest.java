package net.diveon.backend.domain.contest.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAlreadyStartedException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 2026-11-13: participants may join a running contest (festival walk-ins); only ended contests refuse. */
class ContestLateJoinTest {
    private final ContestRepository contests = mock(ContestRepository.class);
    private final ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ContestService service = new ContestService(contests, participants, mock(ContestTagRepository.class),
            mock(ContestProblemRepository.class), mock(ProblemRepository.class), users,
            mock(GroupUserRepository.class), mock(GroupRepository.class));

    private Contest contest(LocalDateTime start, LocalDateTime end) {
        Contest contest = new Contest();
        ReflectionTestUtils.setField(contest, "id", 5L);
        ReflectionTestUtils.setField(contest, "startTime", start);
        ReflectionTestUtils.setField(contest, "endTime", end);
        ReflectionTestUtils.setField(contest, "visibility", Contest.Visibility.PUBLIC);
        when(contests.findById(5L)).thenReturn(Optional.of(contest));
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 9L);
        when(users.findById(9L)).thenReturn(Optional.of(user));
        when(participants.findByContestIdAndUserId(5L, 9L)).thenReturn(Optional.empty());
        return contest;
    }

    @Test void runningContestAcceptsANewParticipant() {
        LocalDateTime now = LocalDateTime.now();
        contest(now.minusHours(1), now.plusHours(1));
        assertThat(service.joinContest(5L, 9L).getIsJoined()).isTrue();
        verify(participants).save(argThat(p -> p.getRole() == ContestParticipant.ContestRole.PARTICIPANT));
    }

    @Test void upcomingContestStillAcceptsParticipants() {
        LocalDateTime now = LocalDateTime.now();
        contest(now.plusHours(1), now.plusHours(2));
        assertThat(service.joinContest(5L, 9L).getIsJoined()).isTrue();
    }

    @Test void endedContestRefusesWithAClearMessage() {
        LocalDateTime now = LocalDateTime.now();
        contest(now.minusHours(2), now.minusHours(1));
        assertThatThrownBy(() -> service.joinContest(5L, 9L))
                .isInstanceOf(ContestAlreadyStartedException.class)
                .hasMessage("종료된 대회에는 참가할 수 없습니다.");
        verify(participants, never()).save(any());
    }
}
