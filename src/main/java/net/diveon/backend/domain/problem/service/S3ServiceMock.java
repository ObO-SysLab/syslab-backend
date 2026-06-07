package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 로컬 환경용. 실제 S3 연결 없이 로그만 찍고 끝
@Service
@Profile("local")
public class S3ServiceMock implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceMock.class);

    @Override
    public void uploadDockerfileZip(Long probId, MultipartFile file) {
        log.info("[Mock] S3 업로드 생략 - probId: {}, 파일명: {}", probId, file.getOriginalFilename());
    }

    @Override
    public void uploadCodingTestcases(Long probId, List<ForDtoTestCase> testcases) {
        int testcaseCount = testcases != null ? testcases.size() : 0;
        log.info("[Mock] 코딩형 테스트케이스 S3 업로드 생략 - probId: {}, testcaseCount: {}, prefix: testcases/prob-{}/",
                probId, testcaseCount, probId);
    }

    @Override
    public void deleteDockerfileZip(Long probId) {
        log.info("[Mock] S3 Dockerfile zip 삭제 생략 - probId: {}, key: prob-{}/source.zip", probId, probId);
    }

    @Override
    public void deleteCodingTestcases(Long probId) {
        log.info("[Mock] S3 코딩형 테스트케이스 삭제 생략 - probId: {}, prefix: testcases/prob-{}/", probId, probId);
    }
}
