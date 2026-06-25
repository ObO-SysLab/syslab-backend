package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemRankingResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProblemRankingService {

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final ProblemRepository problemRepository;

    public ProblemRankingService(SolveSubmissionRepository solveSubmissionRepository,
                                  SolveSubmissionCodingRepository solveSubmissionCodingRepository,
                                  ProblemRepository problemRepository) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.problemRepository = problemRepository;
    }

    public ProblemRankingResponse getRanking(long problemId, int page) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(ProblemNotFoundException::new);

        Page<SolveSubmission> resultPage = solveSubmissionRepository
                .findFirstCorrectByProblemId(problemId, PageRequest.of(page - 1, 20));

        boolean isCoding = "coding".equals(problem.getType());
        int startRank = (page - 1) * 20 + 1;
        List<ProblemRankingResponse.RankingEntry> entries = new ArrayList<>();
        int rank = startRank;

        for (SolveSubmission submission : resultPage.getContent()) {
            String language = null;
            if (isCoding) {
                SolveSubmissionCoding coding = solveSubmissionCodingRepository.findById(submission.getId()).orElse(null);
                if (coding != null) {
                    language = coding.getLanguage();
                }
            }
            entries.add(new ProblemRankingResponse.RankingEntry(
                    rank++,
                    submission.getUser().getId(),
                    submission.getUser().getNickname(),
                    submission.getUser().getProfileImgUrl(),
                    submission.getSubmittedAt(),
                    language
            ));
        }

        return new ProblemRankingResponse(
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                page,
                entries
        );
    }
}
