package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.Map;

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
    public void uploadDockerfileZip(Long probId, String requestId, MultipartFile file, List<String> commandPolicy) {
        try {
            String key = new PracticeBuildIdentity(probId, requestId).sourceKey();
            // Metadata travels with this exact object, so the build it triggers always sees it.
            Map<String, String> metadata = commandPolicy == null ? Map.of()
                    : Map.of(PracticeCommandPolicy.METADATA_KEY, PracticeCommandPolicy.metadataValue(commandPolicy));

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/zip")
                            .metadata(metadata)
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasDockerfileZip(Long probId, String requestId) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket)
                    .key(new PracticeBuildIdentity(probId, requestId).sourceKey()).build());
            return true;
        } catch (NoSuchKeyException missing) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    public void copyDockerfileZip(Long probId, String fromRequestId, String toRequestId, List<String> commandPolicy) {
        Map<String, String> metadata = commandPolicy == null ? Map.of()
                : Map.of(PracticeCommandPolicy.METADATA_KEY, PracticeCommandPolicy.metadataValue(commandPolicy));
        // A copy is an "Object Created" event on a key ending in source.zip, so it starts the build.
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(new PracticeBuildIdentity(probId, fromRequestId).sourceKey())
                .destinationBucket(bucket).destinationKey(new PracticeBuildIdentity(probId, toRequestId).sourceKey())
                .metadataDirective(MetadataDirective.REPLACE).metadata(metadata).contentType("application/zip")
                .build());
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

    @Override
    public void deleteDockerfileZip(Long probId, String requestId) {
        if (requestId == null) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key("prob-" + probId + "/source.zip").build());
            return;
        }
        var identity = new PracticeBuildIdentity(probId, requestId);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(identity.sourceKey()).build());
        // A failed build may have left its reason next to the source; deleting a missing key succeeds.
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(identity.resultKey()).build());
    }

    @Override
    public void deleteCodingTestcases(Long probId) {
        String prefix = "testcases/prob-" + probId + "/";
        List<ObjectIdentifier> keys = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(codeBucket)
                        .prefix(prefix)
                        .build())
                .contents().stream()
                .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                .toList();

        if (keys.isEmpty()) return;

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(codeBucket)
                .delete(Delete.builder().objects(keys).build())
                .build());
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
