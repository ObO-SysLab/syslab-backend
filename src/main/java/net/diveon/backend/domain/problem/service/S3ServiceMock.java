package net.diveon.backend.domain.problem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// 로컬 환경용. 실제 S3 연결 없이 로그만 찍고 끝
@Service
@Profile("local")
public class S3ServiceMock implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceMock.class);

    @Override
    public void uploadDockerfileZip(Long probId, MultipartFile file) {
        log.info("[Mock] S3 업로드 생략 - probId: {}, 파일명: {}", probId, file.getOriginalFilename());
    }
}
