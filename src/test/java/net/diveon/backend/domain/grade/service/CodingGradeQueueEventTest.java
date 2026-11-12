package net.diveon.backend.domain.grade.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

// 작업표에 등록하면 커밋 직후 발송 신호를 낸다. 복구 기능이 꺼진 예전 경로는 그대로 바로 보낸다.
class CodingGradeQueueEventTest {
    private final S3Client s3 = mock(S3Client.class);
    private final SqsClient sqs = mock(SqsClient.class);
    private final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    private final SolveSubmissionCodingRepository codings = mock(SolveSubmissionCodingRepository.class);
    private final ProblemCodingRepository problems = mock(ProblemCodingRepository.class);
    private final CodingGradeJobStore jobs = mock(CodingGradeJobStore.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CodingGradeQueueServiceImpl service = new CodingGradeQueueServiceImpl(s3, sqs, new ObjectMapper(),
            submissions, codings, problems, jobs, mock(EntityManager.class), events);

    private void fixture() {
        when(submissions.findById(55L)).thenReturn(Optional.of(mock(SolveSubmission.class)));
        var code = mock(SolveSubmissionCoding.class);
        when(code.getLanguage()).thenReturn("python");
        when(code.getAnswer()).thenReturn("print(1)");
        when(codings.findById(55L)).thenReturn(Optional.of(code));
        var coding = new ProblemCoding();
        coding.setTimeLimitMs(1000);
        coding.setMemoryLimitMb(128);
        coding.setAllowedLanguages(List.of("python"));
        coding.setTestcases(List.of(new ForDtoTestCase(1, "1", "1", true)));
        when(problems.findById(7L)).thenReturn(Optional.of(coding));
        ReflectionTestUtils.setField(service, "queueUrl", "https://sqs.example/q");
        ReflectionTestUtils.setField(service, "codeBucket", "code");
        ReflectionTestUtils.setField(service, "graceSeconds", 900L);
    }

    @Test
    void registeredJobAsksForImmediateDispatch() {
        fixture();
        ReflectionTestUtils.setField(service, "recoveryEnabled", true);
        service.sendGradeRequest(7L, 3L, 55L);
        var order = inOrder(jobs, events);
        order.verify(jobs).register(eq(55L), anyString(), eq(901L));
        order.verify(events).publishEvent(new CodingGradeRequested(55L));
        verifyNoInteractions(s3, sqs);
    }

    @Test
    void legacyPathStillSendsDirectly() {
        fixture();
        ReflectionTestUtils.setField(service, "recoveryEnabled", false);
        service.sendGradeRequest(7L, 3L, 55L);
        verify(sqs).sendMessage(any(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class));
        verifyNoInteractions(events, jobs);
    }
}
