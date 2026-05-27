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
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ContestSubmissionStatusService(
            ContestSubmissionRepository contestSubmissionRepository,
            StringRedisTemplate redisTemplate,
            JdbcTemplate jdbcTemplate) {
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
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
            return ContestSubmissionStatusResponse.completed(submissionId, isCorrect, score, rank);
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
        try {
            Long zeroBasedRank = redisTemplate.opsForZSet()
                    .reverseRank("scoreboard:" + contestId, String.valueOf(userId));
            if (zeroBasedRank != null) {
                return zeroBasedRank.intValue() + 1;
            }
        } catch (Exception ignored) {}
        Integer higherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM contest_participant WHERE contest_id = ? AND score > ?",
                Integer.class, contestId, score);
        return (higherCount != null ? higherCount : 0) + 1;
    }

    private int calculateProgress(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "JUDGING" -> 50;
            case "COMPLETED" -> 100;
            default -> 0;
        };
    }
}
