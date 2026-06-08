package net.diveon.backend.global.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// 로컬 환경용. 실제 S3 업로드 없이 로그만 찍고 mock URL을 반환
@Service
@Profile("local")
public class ImageUploadServiceMock implements ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadServiceMock.class);

    @Override
    public String upload(String key, MultipartFile file) {
        log.info("[Mock] 이미지 업로드 생략 - key: {}, 파일명: {}", key, file.getOriginalFilename());
        return "https://mock.cloudfront.net/" + key;
    }
}
