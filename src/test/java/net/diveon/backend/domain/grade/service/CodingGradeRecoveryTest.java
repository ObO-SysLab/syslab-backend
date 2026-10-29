package net.diveon.backend.domain.grade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.grade.dto.message.CodingGradeMessage;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodingGradeRecoveryTest {
    private final CodingGradeJobStore jobs = mock(CodingGradeJobStore.class);
    private final CodingGradeQueueServiceImpl queue = mock(CodingGradeQueueServiceImpl.class);
    private final SolveSubmissionCodingRepository submissions = mock(SolveSubmissionCodingRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final CodingGradeRecovery recovery = new CodingGradeRecovery(jobs, queue, submissions, mapper, 3);

    private CodingGradeJobStore.Dispatch job() throws Exception {
        var message = new CodingGradeMessage("101", "key", "python", "2", 1, 128, 2);
        message.setContestContext(new CodingGradeMessage.ContestContext(4L, 9L, 100));
        return new CodingGradeJobStore.Dispatch(101, 2, mapper.writeValueAsString(message));
    }

    @Test void restoresAttemptAndContestContext() throws Exception {
        when(jobs.claimNext(3)).thenReturn(job()).thenReturn(null);
        var code = mock(SolveSubmissionCoding.class);
        when(code.getAnswer()).thenReturn("print(1)");
        when(submissions.findById(101L)).thenReturn(Optional.of(code));
        recovery.dispatchDueJobs();
        var captor = org.mockito.ArgumentCaptor.forClass(CodingGradeMessage.class);
        verify(queue).dispatch(captor.capture(), eq("print(1)"));
        assertEquals(2, captor.getValue().getAttemptNo());
        assertEquals(9L, captor.getValue().getContestContext().getContestProblemId());
    }

    @Test void sendFailureDefersNextAttemptUntilNextTick() throws Exception {
        var job = job();
        when(jobs.claimNext(3)).thenReturn(job);
        when(submissions.findById(101L)).thenReturn(Optional.empty());
        recovery.dispatchDueJobs();
        verify(jobs).dispatchFailed(job);
        verify(jobs, times(1)).claimNext(3);
        verifyNoInteractions(queue);
    }

    @Test void terminalJobIsNeverSent() {
        when(jobs.claimNext(3)).thenReturn(new CodingGradeJobStore.Dispatch(101, 3, null)).thenReturn(null);
        recovery.dispatchDueJobs();
        verifyNoInteractions(queue, submissions);
    }

    @Test void unboundedRetriesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CodingGradeRecovery(jobs, queue, submissions, mapper, 4));
    }
}
