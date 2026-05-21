package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;

public interface CodingGradeQueueService {
    void sendGradeRequest(long probId, long submitterId, long submissionId);
    void sendContestGradeRequest(long probId, long submitterId, long submissionId, CodingGradeMessage.ContestContext contestContext);
}
