package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.group.entity.*;
import net.diveon.backend.domain.group.repository.*;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class ProblemAccessServiceTest {
    ProblemRepository problems = mock(ProblemRepository.class);
    GroupProblemRepository groups = mock(GroupProblemRepository.class);
    GroupUserRepository members = mock(GroupUserRepository.class);
    ContestProblemRepository contests = mock(ContestProblemRepository.class);
    ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2026-10-30T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    LocalDateTime now = LocalDateTime.now(clock);
    ProblemAccessService service = new ProblemAccessService(problems, groups, members, contests, participants, clock);
    Problem problem;
    User owner = new User();
    @BeforeEach void setup() { ReflectionTestUtils.setField(owner, "id", 1L); }

    void problem(String visibility) {
        problem = new Problem(owner, "practice", "fixture", "OS", "easy", visibility);
        ReflectionTestUtils.setField(problem, "id", 9L);
        when(problems.findById(9L)).thenReturn(Optional.of(problem));
    }
    void contest(LocalDateTime start, LocalDateTime end, boolean joined, boolean banned) {
        Contest contest = new Contest(); ReflectionTestUtils.setField(contest, "id", 5L);
        contest.updateTime(start, end);
        var participant = new ContestParticipant(contest, owner, ContestParticipant.ContestRole.PARTICIPANT);
        if (banned) participant.ban();
        when(participants.findByContestIdAndUserId(5L, 7L)).thenReturn(joined ? Optional.of(participant) : Optional.empty());
        when(contests.findAllWithContestByProblemId(9L)).thenReturn(List.of(new ContestProblem(contest, problem, 100)));
    }
    void denyReadAndRun() {
        assertThrows(ProblemNotFoundException.class, () -> service.requireRead(7L, 9L));
        assertThrows(ProblemNotFoundException.class, () -> service.requirePractice(7L, 9L));
    }

    @Test void publicAllowsAuthenticatedReadAndExecute() {
        problem("public");
        assertSame(problem, service.requireRead(7L, 9L));
        assertSame(problem, service.requirePractice(7L, 9L));
        service.requireNormalSubmission(7L, problem);
        assertThrows(ProblemNotFoundException.class, () -> service.requireRead(null, 9L));
        verifyNoInteractions(groups, contests);
    }
    @Test void privateAllowsOnlyOwnerEvenWhenAttachedElsewhere() {
        problem("private");
        service.requireRead(1L, 9L); service.requirePractice(1L, 9L); service.requireNormalSubmission(1L, problem);
        denyReadAndRun();
        assertThrows(ProblemNotFoundException.class, () -> service.requireNormalSubmission(7L, problem));
        assertThrows(ProblemNotFoundException.class, () -> service.requireContestSubmission(7L, problem, 5L));
        verifyNoInteractions(groups, contests);
    }
    @Test void unknownVisibilityAndMissingProblemFailClosed() {
        problem("unexpected");
        assertThrows(ProblemNotFoundException.class, () -> service.requireRead(1L, 9L));
        assertThrows(ProblemNotFoundException.class, () -> service.requireRead(7L, 10L));
    }
    @Test void groupMembershipIsRecheckedAndOwnerOnlyHasReadException() {
        problem("group"); Group group = new Group(); ReflectionTestUtils.setField(group, "id", 5L);
        when(groups.findAllByProblemId(9L)).thenReturn(List.of(new GroupProblem(problem, group)));
        when(members.existsByGroupIdAndUserId(5L, 7L)).thenReturn(true);
        service.requireRead(7L, 9L); service.requirePractice(7L, 9L); service.requireNormalSubmission(7L, problem);
        when(members.existsByGroupIdAndUserId(5L, 7L)).thenReturn(false);
        denyReadAndRun(); service.requireRead(1L, 9L);
        assertThrows(ProblemNotFoundException.class, () -> service.requirePractice(1L, 9L));
    }
    @Test void unlinkedGroupAndContestProblemsAreNotPublic() {
        for (String visibility : List.of("group", "contest")) { problem(visibility); denyReadAndRun(); }
    }
    @Test void upcomingContestIsHiddenExceptOwnerManagementRead() {
        problem("contest"); contest(now.plusHours(1), now.plusHours(2), true, false);
        denyReadAndRun(); service.requireRead(1L, 9L);
        assertThrows(ProblemNotFoundException.class, () -> service.requirePractice(1L, 9L));
    }
    @Test void ongoingContestRequiresMatchingSubmissionContext() {
        problem("contest"); contest(now, now.plusHours(1), true, false);
        service.requireRead(7L, 9L); service.requirePractice(7L, 9L); service.requireContestSubmission(7L, problem, 5L);
        assertThrows(ProblemNotFoundException.class, () -> service.requireContestSubmission(7L, problem, 6L));
        assertThrows(ProblemNotFoundException.class, () -> service.requireNormalSubmission(7L, problem));
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void bannedOrNonparticipantCannotReadOrExecute(boolean banned) {
        problem("contest"); contest(now.minusHours(1), now.plusHours(1), banned, banned); denyReadAndRun();
    }
    @Test void endingAtCurrentTimeAllowsReadButStopsExecution() {
        problem("contest"); contest(now.minusHours(1), now, true, false); service.requireRead(7L, 9L);
        assertThrows(ProblemNotFoundException.class, () -> service.requirePractice(7L, 9L));
        assertThrows(ProblemNotFoundException.class, () -> service.requireContestSubmission(7L, problem, 5L));
    }
    @Test void malformedDatesAndNonpracticeTypeCannotRun() {
        problem("contest"); contest(now.plusHours(1), now.minusHours(1), true, false); denyReadAndRun();
        problem("public"); ReflectionTestUtils.setField(problem, "type", "coding");
        assertThrows(ProblemNotFoundException.class, () -> service.requirePractice(7L, 9L));
    }
}
