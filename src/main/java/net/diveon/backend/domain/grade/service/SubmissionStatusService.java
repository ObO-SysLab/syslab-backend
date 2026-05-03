package net.diveon.backend.domain.grade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.dto.response.SubmissionStatusResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;

@Service
public class SubmissionStatusService {

    private final SolveSubmissionRepository solveSubmissionRepository;

    public SubmissionStatusService(SolveSubmissionRepository solveSubmissionRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionStatusResponse getStatus(Long userId, Long submissionId) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            // TODO: 서비스 고도화 시 SubmissionNotFoundException 같은 전용 예외로 분리할 것.
            .orElseThrow(() -> new RuntimeException("제출 상태 조회 실패: 제출 정보가 존재하지 않습니다."));

        if (!submission.getUser().getId().equals(userId)) {
            // TODO: 서비스 고도화 시 SubmissionAccessDeniedException 같은 전용 예외로 분리할 것.
            throw new RuntimeException("제출 상태 조회 실패: 해당 제출 정보에 접근할 권한이 없습니다.");
        }

        SubmissionState submissionState = submission.getSubmissionState();
        String status = submissionState.name();
        int progress = calculateProgress(submissionState);

        return new SubmissionStatusResponse(
            String.valueOf(submission.getId()),
            submission.getProblem().getId(),
            submission.getProblem().getType(),
            status,
            progress
        );
    }

    private int calculateProgress(SubmissionState submissionState) {
        return switch (submissionState) {
            case PENDING -> 0;
            case JUDGING -> 60;
            case COMPLETED -> 100;
        };
    }
}
