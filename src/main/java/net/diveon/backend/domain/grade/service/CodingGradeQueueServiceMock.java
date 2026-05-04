package net.diveon.backend.domain.grade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveResult.SolveResultState;
import net.diveon.backend.domain.grade.entity.SolveResultCoding;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.grade.repository.SolveResultCodingRepository;

@Service
@Profile("local")
@Transactional
public class CodingGradeQueueServiceMock implements CodingGradeQueueService {

    private static final Logger log = LoggerFactory.getLogger(CodingGradeQueueServiceMock.class);

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveResultRepository solveResultRepository;
    private final SolveResultCodingRepository solveResultCodingRepository;

    public CodingGradeQueueServiceMock(
        SolveSubmissionRepository solveSubmissionRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveResultRepository solveResultRepository,
        SolveResultCodingRepository solveResultCodingRepository
    ) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveResultRepository = solveResultRepository;
        this.solveResultCodingRepository = solveResultCodingRepository;
    }

    @Override
    public void sendGradeRequest(long probId, long submitterId, long submissionId) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("제출 정보가 없습니다."));
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("코딩형 제출 정보가 없습니다."));

        log.info(
            "[Mock] 코딩 채점 요청 생략 - probId: {}, submitterId: {}, submissionId: {}, language: {}",
            probId,
            submitterId,
            submissionId,
            submissionCoding.getLanguage()
        );

        SolveResultState resultState = getMockResultState(submissionCoding.getLanguage());
        SolveResult result = new SolveResult(
            submission,
            resultState
        );
        solveResultRepository.save(result);

        SolveResultCoding resultCoding = new SolveResultCoding(
            result,
            getMockScore(resultState),
            0,
            0,
            submissionCoding.getAnswer().length()
        );
        solveResultCodingRepository.save(resultCoding);

        submission.setSubmissionState(SubmissionState.COMPLETED);
    }

    private SolveResultState getMockResultState(String language) {
        if ("python".equals(language)) {
            return SolveResultState.CORRECT;
        }
        return SolveResultState.WRONG;
    }

    private Short getMockScore(SolveResultState resultState) {
        if (resultState == SolveResultState.CORRECT) {
            return 100;
        }
        return 0;
    }
}
