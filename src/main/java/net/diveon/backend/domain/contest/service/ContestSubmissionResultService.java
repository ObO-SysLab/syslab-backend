package net.diveon.backend.domain.contest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.response.ContestSubmissionResultResponse;
import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.SubmissionAccessDeniedException;
import net.diveon.backend.global.exception.SubmissionNotFoundException;

@Service
public class ContestSubmissionResultService {

    private final ContestSubmissionRepository contestSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;

    public ContestSubmissionResultService(
            ContestSubmissionRepository contestSubmissionRepository,
            SolveSubmissionCodingRepository solveSubmissionCodingRepository) {
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
    }

    @Transactional(readOnly = true)
    public ContestSubmissionResultResponse getResult(Long contestId, Long submissionId, Long userId) {
        ContestSubmission submission = contestSubmissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        if (!submission.getUser().getId().equals(userId)) {
            throw new SubmissionAccessDeniedException();
        }

        if (!submission.getContest().getId().equals(contestId)) {
            throw new ContestNotFoundException();
        }

        if (submission.getSolveSubmission() == null) {
            throw new SubmissionNotFoundException();
        }

        SolveSubmissionCoding coding = solveSubmissionCodingRepository
                .findById(submission.getSolveSubmission().getId())
                .orElseThrow(SubmissionNotFoundException::new);

        return new ContestSubmissionResultResponse(submissionId, coding.getLanguage(), coding.getAnswer());
    }
}
