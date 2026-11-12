package net.diveon.backend.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {

    // S3에 이미지를 업로드하고 CloudFront URL을 반환한다 (같은 key로 업로드 시 덮어쓰기)
    String upload(String key, MultipartFile file);

    // 같은 key의 객체를 지운다. 없는 key여도 성공으로 본다.
    void delete(String key);
}
