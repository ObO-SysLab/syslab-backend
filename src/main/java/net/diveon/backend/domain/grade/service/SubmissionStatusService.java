package net.diveon.backend.domain.grade.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.grade.dto.response.SubmissionStatusResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.global.exception.SubmissionAccessDeniedException;
import net.diveon.backend.global.exception.SubmissionNotFoundException;

@Service
public class SubmissionStatusService {

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;

    public SubmissionStatusService(SolveSubmissionRepository solveSubmissionRepository,
                                   ContestSubmissionRepository contestSubmissionRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionStatusResponse getStatus(Long userId, Long submissionId) {
        // 일반 제출 먼저 조회
        Optional<SolveSubmission> solveSubmission = solveSubmissionRepository.findById(submissionId);
        if (solveSubmission.isPresent()) {
            SolveSubmission submission = solveSubmission.get();
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

        // 대회 제출 fallback (contest_submission.id로 polling하는 경우)
        ContestSubmission contestSubmission = contestSubmissionRepository.findById(submissionId)
            .orElseThrow(SubmissionNotFoundException::new);

        if (!contestSubmission.getUser().getId().equals(userId)) {
            throw new SubmissionAccessDeniedException();
        }

        String status = contestSubmission.getSubmissionStatus();
        return new SubmissionStatusResponse(
            String.valueOf(contestSubmission.getId()),
            contestSubmission.getContestProblem().getProblem().getId(),
            contestSubmission.getContestProblem().getProblem().getType(),
            status,
            calculateProgress(status)
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
