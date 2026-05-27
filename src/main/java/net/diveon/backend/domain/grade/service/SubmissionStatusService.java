package net.diveon.backend.domain.grade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.dto.response.SubmissionStatusResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.global.exception.SubmissionAccessDeniedException;
import net.diveon.backend.global.exception.SubmissionNotFoundException;

@Service
public class SubmissionStatusService {

    private final SolveSubmissionRepository solveSubmissionRepository;

    public SubmissionStatusService(SolveSubmissionRepository solveSubmissionRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionStatusResponse getStatus(Long userId, Long submissionId) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(SubmissionNotFoundException::new);

        if (!submission.getUser().getId().equals(userId)) {
            throw new SubmissionAccessDeniedException();
        }

        SubmissionState state = submission.getSubmissionState();
        return new SubmissionStatusResponse(
            String.valueOf(submission.getId()),
            submission.getProblem().getId(),
            submission.getProblem().getType(),
            state.name(),
            calculateProgress(state.name())
        );
    }

    private int calculateProgress(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "JUDGING" -> 60;
            case "COMPLETED" -> 100;
            default -> 0;
        };
    }
}
