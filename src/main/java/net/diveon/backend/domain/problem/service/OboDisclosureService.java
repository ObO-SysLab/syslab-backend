package net.diveon.backend.domain.problem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explanation access is stricter than problem access and uses server-owned contest membership. */
@Service
@Transactional(readOnly = true)
public class OboDisclosureService {
    private final ContestProblemRepository contestProblems;
    private final ContestParticipantRepository participants;
    private final SolveSubmissionRepository submissions;
    private final Clock clock;

    @Autowired
    public OboDisclosureService(ContestProblemRepository contestProblems,
            ContestParticipantRepository participants, SolveSubmissionRepository submissions) {
        this(contestProblems, participants, submissions, Clock.systemDefaultZone());
    }

    OboDisclosureService(ContestProblemRepository contestProblems,
            ContestParticipantRepository participants, SolveSubmissionRepository submissions, Clock clock) {
        this.contestProblems = contestProblems;
        this.participants = participants;
        this.submissions = submissions;
        this.clock = clock;
    }

    public boolean canReadExplanation(Long userId, Long problemId) {
        return status(userId,problemId).allowed();
    }

    public record DisclosureStatus(boolean allowed,String reason,LocalDateTime availableAt) {}

    public DisclosureStatus status(Long userId,Long problemId) {
        if (userId == null || userId <= 0 || problemId == null || problemId <= 0) return new DisclosureStatus(false,"awaiting_grade",null);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime availableAt=null;boolean locked=false,unknownEnd=false;
        for (var link : contestProblems.findAllWithContestByProblemId(problemId)) {
            var contest = link.getContest();
            var start = contest.getStartTime();
            var end = contest.getEndTime();
            boolean invalidPeriod = start == null || end == null || !start.isBefore(end);
            boolean active = invalidPeriod || (!now.isBefore(start) && now.isBefore(end));
            // Banning a participant must not unlock the general-study explanation.
            if (active && participants.findByContestIdAndUserId(contest.getId(), userId).isPresent()) {
                locked=true;
                if(invalidPeriod)unknownEnd=true;
                else if(availableAt==null||end.isAfter(availableAt))availableAt=end;
            }
        }
        if(locked)return new DisclosureStatus(false,"contest",unknownEnd?null:availableAt);
        boolean completed=submissions.countDisclosureEligible(userId, problemId, now) > 0
            || submissions.countCompletedContestForDisclosure(userId, problemId, now) > 0;
        return new DisclosureStatus(completed,completed?null:"awaiting_grade",null);
    }

    public void requireExplanation(Long userId, Long problemId) {
        if (!canReadExplanation(userId, problemId)) throw new ProblemNotFoundException();
    }

    public void requireAuthor(Long userId, Problem problem) {
        if (userId == null || problem.getAuthor() == null
                || !Objects.equals(userId, problem.getAuthor().getId())) throw new ProblemNotFoundException();
    }

    public void requireContestEnded(net.diveon.backend.domain.contest.entity.Contest contest) {
        var start = contest.getStartTime(); var end = contest.getEndTime();
        if (start == null || end == null || !start.isBefore(end) || LocalDateTime.now(clock).isBefore(end))
            throw new ProblemNotFoundException();
    }
}
