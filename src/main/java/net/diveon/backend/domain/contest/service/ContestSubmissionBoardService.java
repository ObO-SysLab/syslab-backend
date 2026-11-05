package net.diveon.backend.domain.contest.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import net.diveon.backend.domain.contest.dto.response.ContestSubmissionBoardResponse;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.service.SubmissionOboPresence;
import net.diveon.backend.domain.grade.service.SubmissionResultStatus;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class ContestSubmissionBoardService {
    private final ContestProblemRepository problems;
    private final ContestParticipantRepository participants;
    private final ContestSubmissionRepository submissions;
    private final SolveSubmissionCodingRepository codingSubmissions;
    private final SolveResultRepository results;
    private final SolveResultCodingRepository codingResults;
    private final ProblemAccessService access;
    private final SubmissionOboPresence oboPresence;
    private final Clock clock;

    @Autowired
    public ContestSubmissionBoardService(ContestProblemRepository problems, ContestParticipantRepository participants,
            ContestSubmissionRepository submissions, SolveSubmissionCodingRepository codingSubmissions,
            SolveResultRepository results, SolveResultCodingRepository codingResults, ProblemAccessService access,
            SubmissionOboPresence oboPresence) {
        this(problems, participants, submissions, codingSubmissions, results, codingResults, access, oboPresence, Clock.systemDefaultZone());
    }
    ContestSubmissionBoardService(ContestProblemRepository problems, ContestParticipantRepository participants,
            ContestSubmissionRepository submissions, SolveSubmissionCodingRepository codingSubmissions,
            SolveResultRepository results, SolveResultCodingRepository codingResults, ProblemAccessService access,
            SubmissionOboPresence oboPresence, Clock clock) {
        this.problems = problems; this.participants = participants; this.submissions = submissions;
        this.codingSubmissions = codingSubmissions; this.results = results; this.codingResults = codingResults;
        this.access = access; this.oboPresence = oboPresence; this.clock = clock;
    }
    public ContestSubmissionBoardResponse getBoard(Long userId, Long contestId, Long contestProblemId, int page, int size) {
        if (userId == null || userId <= 0 || contestId == null || contestId <= 0 || contestProblemId == null || contestProblemId <= 0) throw denied();
        if (page < 1 || page > 1000000 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 범위가 올바르지 않습니다.");
        var link = problems.findByIdAndContestId(contestProblemId, contestId).orElseThrow(ContestSubmissionBoardService::denied);
        var contest = link.getContest();
        if (contest.getStartTime() == null || contest.getEndTime() == null
                || !contest.getStartTime().isBefore(contest.getEndTime())
                || LocalDateTime.now(clock).isBefore(contest.getStartTime())) throw denied();
        var participant = participants.findByContestIdAndUserId(contestId, userId)
                .filter(p -> Boolean.FALSE.equals(p.getIsBanned())).orElseThrow(ContestSubmissionBoardService::denied);
        access.requireReadInContest(userId, link.getProblem().getId(), contestId);
        var found = submissions.findByContestIdAndContestProblemIdAndUserId(contestId, contestProblemId, userId,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "submittedAt", "id")));
        var items = found.getContent().stream().map(s -> item(s, link, userId)).toList();
        return new ContestSubmissionBoardResponse(contestId, contestProblemId, link.getProblem().getId(),
                participant.getScore(), page, found.getTotalPages(), found.getTotalElements(), items);
    }
    private ContestSubmissionBoardResponse.Item item(ContestSubmission row, ContestProblem link, Long userId) {
        if (!Objects.equals(row.getContest().getId(), link.getContest().getId())
                || !Objects.equals(row.getContestProblem().getId(), link.getId()) || !Objects.equals(row.getUser().getId(), userId)) throw denied();
        var solve = row.getSolveSubmission();
        Long solveId = null; String language = null;
        Integer runtime = null, memory = null;
        String status = row.getSubmissionStatus();
        net.diveon.backend.domain.grade.entity.SolveResult result = null;
        if ("coding".equals(link.getProblem().getType()) && solve != null) {
            if (!Objects.equals(solve.getUser().getId(), userId) || !Objects.equals(solve.getProblem().getId(), link.getProblem().getId())) throw denied();
            solveId = solve.getId();
            // The grader updates the linked solve row to JUDGING before the contest row is terminal.
            if (!"COMPLETED".equals(status) && !"FAILED".equals(status) && solve.getSubmissionState() != null)
                status = solve.getSubmissionState().name();
            var coding = codingSubmissions.findById(solveId).orElse(null);
            if (coding != null) language = coding.getLanguage();
            if ("COMPLETED".equals(status)) {
                result = results.findBySubmissionId(solveId).orElse(null);
                var metrics = result == null ? null : codingResults.findById(result.getId()).orElse(null);
                if (metrics != null) { runtime = metrics.getRuntime(); memory = metrics.getMemoryUsage(); }
            }
        }
        if (!"coding".equals(link.getProblem().getType()) && solve != null
                && "COMPLETED".equals(status) && solve.getSubmissionState() == net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED)
            result = results.findBySubmissionId(solve.getId()).orElse(null);
        if (status == null || !java.util.List.of("PENDING", "JUDGING", "COMPLETED", "FAILED").contains(status))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "제출 상태를 확인할 수 없습니다.");
        return new ContestSubmissionBoardResponse.Item(row.getId(), solveId, status,
                "COMPLETED".equals(status) ? row.getIsCorrect() : null, SubmissionResultStatus.fromContest(row, result), row.getSubmittedAt(), row.getUser().getNickname(), language, runtime, memory,
                row.getUser().getTier(), oboPresence.hasObo(link.getProblem(),
                        solve == null ? row.getProblemRevision() : solve.getProblemRevision()));
    }
    private static ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "대회 제출 기록을 찾을 수 없습니다."); }
}
