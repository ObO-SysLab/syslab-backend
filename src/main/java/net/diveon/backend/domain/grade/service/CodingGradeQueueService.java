package net.diveon.backend.domain.grade.service;

public interface CodingGradeQueueService {
    void sendGradeRequest(long probId, long submitterId, long submissionId);
}
