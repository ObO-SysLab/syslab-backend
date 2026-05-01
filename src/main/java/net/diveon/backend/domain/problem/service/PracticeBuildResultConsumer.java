package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


// SQS Worker
@Component
@Profile("prod")
public class PracticeBuildResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(PracticeBuildResultConsumer.class);

    private final SqsClient sqsClient;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.practice-build-url}")
    private String queueUrl;

    @Value("${aws.ecr.practice-registry}")
    private String ecrRegistry;

    @Value("${aws.ecr.practice-repository}")
    private String ecrRepository;

    public PracticeBuildResultConsumer(SqsClient sqsClient,
                                       ProblemPracticeRepository problemPracticeRepository,
                                       ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.problemPracticeRepository = problemPracticeRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000) // 5초마다 SQS 폴링
    public void poll() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(5)
                        .build()
        );

        for (Message message : response.messages()) {
            try {
                // 메시지 Body JSON 파싱
                JsonNode body = objectMapper.readTree(message.body());
                String buildStatus = body.at("/detail/build-status").asText();
                String location = body.at("/detail/additional-information/source/location").asText();

                // location에서 prob_id 추출 (예: syslab-practice-dockerfile/prob-1/source.zip → 1)
                Pattern pattern = Pattern.compile("prob-(\\d+)/");
                Matcher matcher = pattern.matcher(location);
                if (!matcher.find()) {
                    log.warn("prob_id 추출 실패: {}", location);
                    continue;
                }
                Long probId = Long.parseLong(matcher.group(1));

                // 빌드 결과에 따라 DB 업데이트
                if ("SUCCEEDED".equals(buildStatus)) {
                    String imageUri = ecrRegistry + "/" + ecrRepository + ":prob-" + probId;
                    problemPracticeRepository.updateReady(probId, imageUri);
                    log.info("빌드 성공 - prob_id: {}, imageUri: {}", probId, imageUri);
                } else if ("FAILED".equals(buildStatus)) {
                    problemPracticeRepository.updateFailed(probId);
                    log.warn("빌드 실패 - prob_id: {}", probId);
                }

                // 처리 완료된 메시지 SQS에서 삭제
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build()
                );

            } catch (Exception e) {
                // 파싱 실패 시 메시지 삭제 안 함 → 가시성 타임아웃 후 SQS가 재시도
                log.error("SQS 메시지 처리 실패", e);
            }
        }
    }
}
