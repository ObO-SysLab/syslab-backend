package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

// 배포 환경용. 실제 S3에 .zip 파일을 올림
@Service
@Profile("prod")
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-practice-dockerfile}")
    private String bucket;

    @Value("${aws.s3.bucket-code}")
    private String codeBucket;

    public S3ServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void uploadDockerfileZip(Long probId, MultipartFile file) {
        try {
            String key = "prob-" + probId + "/source.zip"; // EventBridge 감지 경로, 형식 변경 금지 (경로 바뀌면 CodeBuild 자동 트리거 불가)

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/zip")
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public void uploadCodingTestcases(Long probId, List<ForDtoTestCase> testcases) {
        if (testcases == null || testcases.isEmpty()) {
            return;
        }

        try {
            for (int i = 0; i < testcases.size(); i++) {
                int testcaseNumber = i + 1;
                ForDtoTestCase testcase = testcases.get(i);
                String prefix = "testcases/prob-" + probId + "/";

                uploadTextFile(prefix + "input_" + testcaseNumber + ".txt", testcase.getInput());
                uploadTextFile(prefix + "output_" + testcaseNumber + ".txt", testcase.getOutput());
            }
        } catch (Exception e) {
            throw new RuntimeException("코딩형 테스트케이스 S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    private void uploadTextFile(String key, String content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(codeBucket)
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromString(content != null ? content : "")
        );
    }
}
