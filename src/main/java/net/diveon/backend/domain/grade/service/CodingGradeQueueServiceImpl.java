package net.diveon.backend.domain.grade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@Profile("prod")
@Transactional
public class CodingGradeQueueServiceImpl implements CodingGradeQueueService {

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final ProblemCodingRepository problemCodingRepository;

    @Value("${aws.sqs.url}")
    private String queueUrl;

    @Value("${aws.s3.bucket-code}")
    private String codeBucket;

    public CodingGradeQueueServiceImpl(
        S3Client s3Client,
        SqsClient sqsClient,
        ObjectMapper objectMapper,
        SolveSubmissionRepository solveSubmissionRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        ProblemCodingRepository problemCodingRepository
    ) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.problemCodingRepository = problemCodingRepository;
    }

    @Override
    public void sendGradeRequest(long probId, long submitterId, long submissionId) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("제출 정보가 없습니다."));
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("코딩형 제출 정보가 없습니다."));
        ProblemCoding problemCoding = problemCodingRepository.findById(probId)
            .orElseThrow(() -> new RuntimeException("코딩형 문제가 없습니다."));

        String language = submissionCoding.getLanguage();
        String s3Key = "submissions/" + submissionId + "/" + getFileName(language);

        uploadCode(s3Key, submissionCoding.getAnswer());
        sendMessage(createMessage(submissionId, s3Key, language, probId, problemCoding));

        submission.setSubmissionState(SubmissionState.RECEIVED);
    }

    private void uploadCode(String s3Key, String code) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(codeBucket)
                .key(s3Key)
                .build(),
            RequestBody.fromString(code)
        );
    }

    private void sendMessage(CodingGradeMessage message) {
        try {
            sqsClient.sendMessage(
                SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .build()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("코딩 채점 요청 메시지 생성에 실패했습니다.", e);
        }
    }

    private CodingGradeMessage createMessage(
        long submissionId,
        String s3Key,
        String language,
        long probId,
        ProblemCoding problemCoding
    ) {
        return new CodingGradeMessage(
            String.valueOf(submissionId),
            s3Key,
            language,
            String.valueOf(probId),
            convertTimeLimitToSeconds(problemCoding.getTimeLimitMs()),
            problemCoding.getMemoryLimitMb(),
            problemCoding.getTestcases().size()
        );
    }

    private int convertTimeLimitToSeconds(Integer timeLimitMs) {
        return (timeLimitMs + 999) / 1000;
    }

    private String getFileName(String language) {
        return switch (language) {
            case "python" -> "Main.py";
            case "c" -> "Main.c";
            case "cpp" -> "Main.cpp";
            default -> throw new IllegalArgumentException("지원하지 않는 언어: " + language);
        };
    }
}
