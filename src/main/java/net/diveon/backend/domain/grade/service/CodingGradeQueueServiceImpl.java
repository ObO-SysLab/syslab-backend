package net.diveon.backend.domain.grade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
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
    private final CodingGradeJobStore jobs;
    private final jakarta.persistence.EntityManager entityManager;

    @Value("${grading.recovery.enabled:false}")
    private boolean recoveryEnabled;

    @Value("${grading.recovery.grace-seconds:900}")
    private long graceSeconds;

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
        ProblemCodingRepository problemCodingRepository,
        CodingGradeJobStore jobs,
        jakarta.persistence.EntityManager entityManager
    ) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.jobs = jobs;
        this.entityManager = entityManager;
    }

    @Override
    public void sendContestGradeRequest(long probId, long submitterId, long submissionId,
                                        CodingGradeMessage.ContestContext contestContext) {
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("제출 정보가 없습니다."));
        SolveSubmissionCoding submissionCoding = solveSubmissionCodingRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("코딩형 제출 정보가 없습니다."));
        ProblemCoding problemCoding = problemCodingRepository.findById(probId)
            .orElseThrow(() -> new RuntimeException("코딩형 문제가 없습니다."));

        String language = submissionCoding.getLanguage();
        String s3Key = "submissions/" + submissionId + "/" + getFileName(language);

        CodingGradeMessage message = createMessage(submissionId, s3Key, language, probId, problemCoding);
        message.setContestContext(contestContext);
        enqueue(message, submissionCoding.getAnswer());
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

        enqueue(createMessage(submissionId, s3Key, language, probId, problemCoding), submissionCoding.getAnswer());
    }

    private void enqueue(CodingGradeMessage message, String code) {
        if (!recoveryEnabled) {
            dispatch(message, code);
            return;
        }
        if (graceSeconds < 60 || message.getTimeLimit() <= 0 || message.getTestcaseCount() <= 0) {
            throw new IllegalArgumentException("Invalid grading time budget or testcases");
        }
        long timeout = Math.addExact(graceSeconds,
                Math.multiplyExact((long) message.getTimeLimit(), message.getTestcaseCount()));
        try {
            // The contest submission and its request become visible in the same commit.
            entityManager.flush();
            jobs.register(Long.parseLong(message.getSubmissionId()), objectMapper.writeValueAsString(message), timeout);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid grading message", e);
        }
    }

    public void dispatch(CodingGradeMessage message, String code) {
        uploadCode(message.getS3Key(), code);
        sendMessage(message);
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
