package net.diveon.backend.domain.problem.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.*;

class PracticeBuildArtifactsTest {
    @Test void uploadAndDeleteUseSameExactRequestKey() {
        var client = mock(S3Client.class);
        var service = new S3ServiceImpl(client);
        ReflectionTestUtils.setField(service, "bucket", "source-bucket");
        service.uploadDockerfileZip(7L, REQUEST, new MockMultipartFile("file", new byte[]{1}));
        service.deleteDockerfileZip(7L, REQUEST);
        String key = new PracticeBuildIdentity(7, REQUEST).sourceKey();
        verify(client).putObject(argThat((PutObjectRequest r) -> key.equals(r.key()) && "source-bucket".equals(r.bucket())), any(RequestBody.class));
        verify(client).deleteObject(argThat((DeleteObjectRequest r) -> key.equals(r.key())));
    }

    @Test void legacySourceDeletionStillWorks() {
        var client = mock(S3Client.class);
        var service = new S3ServiceImpl(client);
        ReflectionTestUtils.setField(service, "bucket", "source-bucket");
        service.deleteDockerfileZip(7L, null);
        verify(client).deleteObject(argThat((DeleteObjectRequest r) -> "prob-7/source.zip".equals(r.key())));
    }

    @Test void newImageCleanupDeletesOnlyBuildTagNotSharedDigest() {
        var client = mock(EcrClient.class);
        when(client.batchDeleteImage(any(BatchDeleteImageRequest.class))).thenReturn(BatchDeleteImageResponse.builder().build());
        var service = new EcrServiceImpl(client);
        ReflectionTestUtils.setField(service, "ecrRepository", "practice");
        service.deleteImage(7L, BUILD);
        verify(client).batchDeleteImage(argThat((BatchDeleteImageRequest r) -> r.imageIds().getFirst().imageDigest() == null
                && PracticeBuildIdentity.imageTag(BUILD).equals(r.imageIds().getFirst().imageTag())));
    }

    @Test void legacyImageCleanupStillUsesProblemTagAndReportsServiceFailure() {
        var client = mock(EcrClient.class);
        when(client.batchDeleteImage(any(BatchDeleteImageRequest.class))).thenReturn(BatchDeleteImageResponse.builder()
                .failures(ImageFailure.builder().failureCode("ImageNotFound").build()).build());
        var service = new EcrServiceImpl(client);
        ReflectionTestUtils.setField(service, "ecrRepository", "practice");
        assertThatThrownBy(() -> service.deleteImage(7L, null)).isInstanceOf(IllegalStateException.class);
        verify(client).batchDeleteImage(argThat((BatchDeleteImageRequest r) -> "prob-7".equals(r.imageIds().getFirst().imageTag())));
    }
}
