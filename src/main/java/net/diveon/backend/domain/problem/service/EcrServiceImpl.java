package net.diveon.backend.domain.problem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.BatchDeleteImageRequest;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("prod")
public class EcrServiceImpl implements EcrService {

    private final EcrClient ecrClient;

    @Value("${aws.ecr.practice-repository}")
    private String ecrRepository;

    public EcrServiceImpl(EcrClient ecrClient) {
        this.ecrClient = ecrClient;
    }

    @Override
    public void deleteImage(Long probId, String buildId, List<String> commandPolicy) {
        // Delete this build's tags, not a digest that may be shared by another problem. A restricted
        // image also carries its policy marker; ECR deletes the image only once no tag is left.
        List<ImageIdentifier> tags = new ArrayList<>();
        tags.add(ImageIdentifier.builder()
                .imageTag(buildId == null ? "prob-" + probId : PracticeBuildIdentity.imageTag(buildId)).build());
        if (buildId != null && commandPolicy != null)
            tags.add(ImageIdentifier.builder().imageTag(PracticeCommandPolicy.markerTag(buildId, commandPolicy)).build());
        var response = ecrClient.batchDeleteImage(BatchDeleteImageRequest.builder()
                .repositoryName(ecrRepository)
                .imageIds(tags)
                .build());
        if (!response.failures().isEmpty()) throw new IllegalStateException("ECR image cleanup failed");
    }
}
