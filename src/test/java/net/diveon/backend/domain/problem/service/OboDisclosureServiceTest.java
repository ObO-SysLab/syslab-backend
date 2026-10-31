package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.Test;

class OboDisclosureServiceTest {
    final LocalDateTime now = LocalDateTime.of(2026, 10, 31, 12, 0);
    final ContestProblemRepository links = mock(ContestProblemRepository.class);
    final ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    final OboDisclosureService service = new OboDisclosureService(links, participants, submissions,
        Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    ContestProblem contest(long id, LocalDateTime start, LocalDateTime end, boolean member) {
        var contest = mock(Contest.class);
        when(contest.getId()).thenReturn(id);
        when(contest.getStartTime()).thenReturn(start);
        when(contest.getEndTime()).thenReturn(end);
        var link = mock(ContestProblem.class);
        when(link.getContest()).thenReturn(contest);
        if (member) when(participants.findByContestIdAndUserId(id, 7L))
            .thenReturn(Optional.of(new ContestParticipant()));
        return link;
    }
    void completed() { when(submissions.countDisclosureEligible(7L, 9L, now)).thenReturn(1L); }

    @Test void noSubmissionCannotRead() {
        assertFalse(service.canReadExplanation(7L, 9L));
        assertThrows(ProblemNotFoundException.class, () -> service.requireExplanation(7L, 9L));
    }
    @Test void ownCompletedStudyAllowsExplanation() {
        completed(); assertTrue(service.canReadExplanation(7L, 9L));
    }
    @Test void completedLegacyContestSubmissionCanQualifyAfterEnd() {
        when(submissions.countCompletedContestForDisclosure(7L,9L,now)).thenReturn(1L);
        assertTrue(service.canReadExplanation(7L,9L));
    }
    @Test void activeContestOverridesPreviouslyCompletedStudy() {
        completed();
        doReturn(List.of(contest(1, now.minusHours(1), now.plusHours(1), true))).when(links).findAllWithContestByProblemId(9L);
        assertFalse(service.canReadExplanation(7L, 9L));
        verifyNoInteractions(submissions);
    }
    @Test void startInclusiveEndExclusive() {
        completed();
        doReturn(List.of(contest(1, now, now.plusHours(1), true))).when(links).findAllWithContestByProblemId(9L);
        assertFalse(service.canReadExplanation(7L, 9L));
        doReturn(List.of(contest(2, now.minusHours(1), now, true))).when(links).findAllWithContestByProblemId(9L);
        assertTrue(service.canReadExplanation(7L, 9L));
    }
    @Test void overlappingContestRemainsLockedUntilBothEnd() {
        completed();
        doReturn(List.of(
            contest(1, now.minusHours(2), now, true), contest(2, now.minusHours(1), now.plusHours(1), true))).when(links).findAllWithContestByProblemId(9L);
        assertFalse(service.canReadExplanation(7L, 9L));
    }
    @Test void lockedStatusReturnsLatestActiveEndWithoutQueryingAnswers() {
        doReturn(List.of(contest(1,now.minusHours(1),now.plusHours(2),true),
            contest(2,now.minusHours(1),now.plusHours(1),true))).when(links).findAllWithContestByProblemId(9L);
        var status=service.status(7L,9L);
        assertFalse(status.allowed());assertEquals("contest",status.reason());assertEquals(now.plusHours(2),status.availableAt());
        verifyNoInteractions(submissions);
    }
    @Test void nonparticipantIsNotLocked() {
        completed();
        doReturn(List.of(contest(1, now.minusHours(1), now.plusHours(1), false))).when(links).findAllWithContestByProblemId(9L);
        assertTrue(service.canReadExplanation(7L, 9L));
    }
    @Test void upcomingContestDoesNotLockStudy() {
        completed();
        doReturn(List.of(contest(1, now.plusHours(1), now.plusHours(2), true))).when(links).findAllWithContestByProblemId(9L);
        assertTrue(service.canReadExplanation(7L, 9L));
    }
    @Test void bannedParticipantStillLocked() {
        var link = contest(1, now.minusHours(1), now.plusHours(1), true);
        var banned = new ContestParticipant(); banned.ban();
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.of(banned));
        doReturn(List.of(link)).when(links).findAllWithContestByProblemId(9L);
        assertFalse(service.canReadExplanation(7L, 9L));
    }
    @Test void invalidContestPeriodFailsClosedForParticipant() {
        completed();
        doReturn(List.of(contest(1, null, null, true))).when(links).findAllWithContestByProblemId(9L);
        assertFalse(service.canReadExplanation(7L, 9L));
    }
    @Test void invalidIdentityCannotRead() {
        assertFalse(service.canReadExplanation(null, 9L));
        assertFalse(service.canReadExplanation(7L, -1L));
        verifyNoInteractions(links, submissions);
    }
    @Test void authorRouteRequiresActualOwnership() {
        var problem = mock(Problem.class); var author = mock(User.class);
        when(problem.getAuthor()).thenReturn(author); when(author.getId()).thenReturn(1L);
        assertThrows(ProblemNotFoundException.class, () -> service.requireAuthor(7L, problem));
        service.requireAuthor(1L, problem);
    }
}
