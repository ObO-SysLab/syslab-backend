package net.diveon.backend.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// 배포 환경용. 프로필/그룹 이미지를 syslab-frontend 버킷(CloudFront 서빙)에 업로드
@Service
@Profile("prod")
public class ImageUploadServiceImpl implements ImageUploadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-frontend}")
    private String bucket;

    @Value("${aws.s3.cloudfront-domain}")
    private String cloudfrontDomain;

    public ImageUploadServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String upload(String key, MultipartFile file) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage(), e);
        }

        return "https://" + cloudfrontDomain + "/" + key;
    }
}
