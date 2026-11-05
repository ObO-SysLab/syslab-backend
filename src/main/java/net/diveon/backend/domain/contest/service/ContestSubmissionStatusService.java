package net.diveon.backend.domain.contest.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.response.ContestSubmissionStatusResponse;
import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.SubmissionAccessDeniedException;
import net.diveon.backend.global.exception.SubmissionNotFoundException;

@Service
public class ContestSubmissionStatusService {

    private final ContestSubmissionRepository contestSubmissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final net.diveon.backend.domain.grade.repository.SolveResultRepository results;

    public ContestSubmissionStatusService(
            ContestSubmissionRepository contestSubmissionRepository,
            StringRedisTemplate redisTemplate,
            JdbcTemplate jdbcTemplate,
            net.diveon.backend.domain.grade.repository.SolveResultRepository results) {
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.results = results;
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ContestSubmissionStatusResponse getStatus(Long contestId, Long submissionId, Long userId) {
        ContestSubmission submission = contestSubmissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        if (!submission.getUser().getId().equals(userId)) {
            throw new SubmissionAccessDeniedException();
        }

        if (!submission.getContest().getId().equals(contestId)) {
            throw new ContestNotFoundException();
        }

        String status = submission.getSubmissionStatus();

        if ("COMPLETED".equals(status)) {
            boolean isCorrect = Boolean.TRUE.equals(submission.getIsCorrect());
            int score = submission.getContest() != null
                    ? getParticipantScore(contestId, userId)
                    : 0;
            int rank = getRank(contestId, userId, score);
            var solve = submission.getSolveSubmission();
            var result = solve != null && solve.getSubmissionState() == net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED
                    ? results.findBySubmissionId(solve.getId()).orElse(null) : null;
            return ContestSubmissionStatusResponse.completed(submissionId, isCorrect,
                    net.diveon.backend.domain.grade.service.SubmissionResultStatus.fromContest(submission, result), score, rank);
        }

        return ContestSubmissionStatusResponse.pending(submissionId, status, calculateProgress(status));
    }

    private int getParticipantScore(Long contestId, Long userId) {
        Integer score = jdbcTemplate.queryForObject(
                "SELECT score FROM contest_participant WHERE contest_id = ? AND user_id = ?",
                Integer.class, contestId, userId);
        return score != null ? score : 0;
    }

    private int getRank(Long contestId, Long userId, int score) {
        Integer rank = jdbcTemplate.queryForObject(ContestRankQuery.SQL, Integer.class, contestId, userId);
        return rank == null ? 0 : rank;
    }

    private int calculateProgress(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "JUDGING" -> 50;
            case "COMPLETED", "FAILED" -> 100;
            default -> 0;
        };
    }
}
