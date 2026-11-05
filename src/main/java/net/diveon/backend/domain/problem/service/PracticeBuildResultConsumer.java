package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@Component
@Profile("prod")
public class PracticeBuildResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(PracticeBuildResultConsumer.class);
    private final SqsClient sqs;
    private final ObjectMapper mapper;
    private final PracticeBuildReconciler reconciler;
    private final PracticeBuildProperties properties;
    private final String queueUrl;

    public PracticeBuildResultConsumer(SqsClient sqs, ObjectMapper mapper, PracticeBuildReconciler reconciler,
            PracticeBuildProperties properties, @Value("${aws.sqs.practice-build-url}") String queueUrl) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.reconciler = reconciler;
        this.properties = properties;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 5000, scheduler = "practiceBuildScheduler")
    public void poll() {
        if (!properties.isEnabled()) return;
        var response = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
                .maxNumberOfMessages(5).waitTimeSeconds(5).build());
        for (var message : response.messages()) {
            try {
                var body = mapper.readTree(message.body());
                if (!"aws.codebuild".equals(body.path("source").asText())
                        || !"CodeBuild Build State Change".equals(body.path("detail-type").asText())
                        || !properties.getProject().equals(body.at("/detail/project-name").asText()))
                    throw new IllegalArgumentException("Unexpected build event");
                // Event status/source is only a hint; verify the build through AWS.
                reconciler.accept(body.at("/detail/build-id").asText());
                sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle()).build());
            } catch (Exception e) {
                // No ack for malformed/legacy events or AWS/DB errors; existing redrive handles DLQ.
                log.warn("Practice build event deferred: message={}, error={}", message.messageId(), e.getClass().getSimpleName());
            }
        }
    }
}
