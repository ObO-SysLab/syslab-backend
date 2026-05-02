package net.diveon.backend.domain.grade.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.entity.SovleResult;
import net.diveon.backend.domain.grade.entity.SovleResult.SovleResultState;
import net.diveon.backend.domain.grade.entity.SovleResultCoding;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.grade.repository.SovleResultCodingRepository;

@Service
@Profile("local")
@Transactional
public class CodingGradeQueueServiceMock implements CodingGradeQueueService {

    private static final Logger log = LoggerFactory.getLogger(CodingGradeQueueServiceMock.class);

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveResultRepository solveResultRepository;
    private final SovleResultCodingRepository sovleResultCodingRepository;

    public CodingGradeQueueServiceMock(
        SolveSubmissionRepository solveSubmissionRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveResultRepository solveResultRepository,
        SovleResultCodingRepository sovleResultCodingRepository
    ) {
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveResultRepository = solveResultRepository;
        this.sovleResultCodingRepository = sovleResultCodingRepository;
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

        SovleResultState resultState = getMockResultState(submissionCoding.getLanguage());
        SovleResult result = new SovleResult(
            submission,
            resultState,
            getMockMessage(resultState)
        );
        solveResultRepository.save(result);

        SovleResultCoding resultCoding = new SovleResultCoding(
            result,
            getMockScore(resultState),
            0,
            0,
            submissionCoding.getAnswer().length()
        );
        sovleResultCodingRepository.save(resultCoding);

        submission.setSubmissionState(SubmissionState.COMPLETED);
    }

    private SovleResultState getMockResultState(String language) {
        if ("python".equals(language)) {
            return SovleResultState.CORRECT;
        }
        return SovleResultState.WRONG_ANSWER;
    }

    private String getMockMessage(SovleResultState resultState) {
        if (resultState == SovleResultState.CORRECT) {
            return "[Mock] 정답입니다.";
        }
        return "[Mock] python 외 언어는 오답으로 처리됩니다.";
    }

    private Short getMockScore(SovleResultState resultState) {
        if (resultState == SovleResultState.CORRECT) {
            return 100;
        }
        return 0;
    }
}
