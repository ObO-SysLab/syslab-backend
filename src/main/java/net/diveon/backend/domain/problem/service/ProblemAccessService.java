package net.diveon.backend.domain.problem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.group.repository.GroupProblemRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared content access policy. Denial deliberately has the same 404 as absence. */
@Service
@Transactional(readOnly = true)
public class ProblemAccessService {
    private final ProblemRepository problems;
    private final GroupProblemRepository groupProblems;
    private final GroupUserRepository groupUsers;
    private final ContestProblemRepository contestProblems;
    private final ContestParticipantRepository participants;
    private final Clock clock;

    @Autowired
    public ProblemAccessService(ProblemRepository problems, GroupProblemRepository groupProblems,
            GroupUserRepository groupUsers, ContestProblemRepository contestProblems,
            ContestParticipantRepository participants) {
        this(problems, groupProblems, groupUsers, contestProblems, participants, Clock.systemDefaultZone());
    }

    ProblemAccessService(ProblemRepository problems, GroupProblemRepository groupProblems,
            GroupUserRepository groupUsers, ContestProblemRepository contestProblems,
            ContestParticipantRepository participants, Clock clock) {
        this.problems = problems; this.groupProblems = groupProblems; this.groupUsers = groupUsers;
        this.contestProblems = contestProblems; this.participants = participants; this.clock = clock;
    }

    public Problem requireRead(Long userId, Long problemId) {
        return requireReadInContest(userId, problemId, null);
    }

    public Problem requireReadInContest(Long userId, Long problemId, Long contestId) {
        Problem problem = load(problemId);
        authorize(userId, problem, false, contestId);
        return problem;
    }

    public void requireNormalSubmission(Long userId, Problem problem) {
        // Contest-only problems must use the contest submission route and its score context.
        if ("contest".equals(problem.getVisibility())) throw denied();
        authorize(userId, problem, true, null);
    }

    public void requireContestSubmission(Long userId, Problem problem, Long contestId) {
        authorize(userId, problem, true, contestId);
    }

    public Problem requirePractice(Long userId, Long problemId) {
        Problem problem = load(problemId);
        if (!"practice".equals(problem.getType())) throw denied();
        authorize(userId, problem, true, null);
        return problem;
    }

    private Problem load(Long id) {
        if (id == null || id <= 0) throw denied();
        return problems.findById(id).orElseThrow(ProblemAccessService::denied);
    }

    private void authorize(Long userId, Problem problem, boolean execute, Long contestId) {
        if (userId == null || userId <= 0 || problem == null || problem.getVisibility() == null) throw denied();
        boolean owner = problem.getAuthor() != null && Objects.equals(userId, problem.getAuthor().getId());
        boolean allowed = switch (problem.getVisibility()) {
            case "public" -> true;
            case "private" -> owner;
            case "group" -> (!execute && owner) || groupProblems.findAllByProblemId(problem.getId()).stream()
                    .anyMatch(link -> groupUsers.existsByGroupIdAndUserId(link.getGroup().getId(), userId));
            case "contest" -> (!execute && owner) || contestProblems.findAllWithContestByProblemId(problem.getId()).stream()
                    .anyMatch(link -> (contestId == null || Objects.equals(contestId, link.getContest().getId()))
                            && contestAccess(userId, link.getContest(), execute));
            default -> false;
        };
        if (!allowed) throw denied();
    }

    private boolean contestAccess(Long userId, Contest contest, boolean execute) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (contest.getStartTime() == null || contest.getEndTime() == null
                || !contest.getStartTime().isBefore(contest.getEndTime())
                || now.isBefore(contest.getStartTime())
                || (execute && !now.isBefore(contest.getEndTime()))) return false;
        return participants.findByContestIdAndUserId(contest.getId(), userId)
                .filter(p -> Boolean.FALSE.equals(p.getIsBanned())).isPresent();
    }

    private static ProblemNotFoundException denied() { return new ProblemNotFoundException(); }
}
