package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** submissionId always identifies contest_submission; solveSubmissionId is only a coding detail link. */
public record ContestSubmissionBoardResponse(Long contestId, Long contestProblemId, Long probId,
        int currentTotalScore, int page, int totalPages, long total, List<Item> submissions) {
    public record Item(Long submissionId, Long solveSubmissionId, String submissionStatus, Boolean isCorrect, String resultStatus,
            LocalDateTime submittedAt, String nickname, String language, Integer runtime, Integer memoryUsage,
            Integer userTier, boolean hasObo) {}
}
