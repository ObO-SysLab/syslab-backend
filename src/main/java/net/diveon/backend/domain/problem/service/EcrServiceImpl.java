package net.diveon.backend.domain.problem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.BatchDeleteImageRequest;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;

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
    public void deleteImage(Long probId) {
        ecrClient.batchDeleteImage(BatchDeleteImageRequest.builder()
                .repositoryName(ecrRepository)
                .imageIds(ImageIdentifier.builder()
                        .imageTag("prob-" + probId)
                        .build())
                .build());
    }
}
