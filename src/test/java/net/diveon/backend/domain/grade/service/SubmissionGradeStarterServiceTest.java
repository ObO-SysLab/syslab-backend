package net.diveon.backend.domain.grade.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionGradeStarterServiceTest {
    @Mock SolveResultRepository resultRepository;
    @Mock SolveSubmissionRepository submissionRepository;
    @Mock SolveSubmissionObjectiveRepository objectiveSubmissionRepository;
    @Mock SolveSubmissionCodingRepository codingSubmissionRepository;
    @Mock SolveSubmissionPracticeRepository practiceSubmissionRepository;
    @Mock UserRepository userRepository;
    @Mock ProblemRepository problemRepository;
    @Mock ProblemCodingRepository codingRepository;
    @Mock ProblemObjectiveRepository objectiveRepository;
    @Mock ProblemPracticeRepository practiceRepository;
    @InjectMocks SubmissionGradeStarterService service;

    @BeforeEach
    void userExists() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
    }

    private SubmissionGradeReqeust request(String type, Object answer) {
        Problem problem = new Problem(new User(), type, "test", "OS", "easy", "public");
        when(problemRepository.findById(2L)).thenReturn(Optional.of(problem));
        if ("coding".equals(type) && answer instanceof String code && !code.isBlank()) {
            ProblemCoding coding = mock(ProblemCoding.class);
            when(coding.getAllowedLanguages()).thenReturn(List.of("python"));
            when(codingRepository.findById(2L)).thenReturn(Optional.of(coding));
        }
        SubmissionGradeReqeust request = new SubmissionGradeReqeust();
        ReflectionTestUtils.setField(request, "prodId", 2L);
        ReflectionTestUtils.setField(request, "answer", answer);
        ReflectionTestUtils.setField(request, "language", "python");
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"objective", "coding", "practice"})
    void acceptedSubmissionCountsOnceEvenAfterPreviousCorrectAnswer(String type) {
        // The old code skipped counting submissions by users who already solved it.
        lenient().when(resultRepository.existsCorrectResultByUserIdAndProblemId(7L, 2L)).thenReturn(true);
        var request = request(type, "objective".equals(type) ? List.of(1) : "answer");
        service.submissionGradeStart(7L, request);
        verify(problemRepository).incrementSubmittedCount(2L);
        verify(problemRepository, never()).save(any());
        verifyNoInteractions(resultRepository);
    }

    @Test
    void separateAcceptedRequestsEachCountOnce() {
        var request = request("coding", "print(1)");
        service.submissionGradeStart(7L, request);
        service.submissionGradeStart(7L, request);
        verify(problemRepository, times(2)).incrementSubmittedCount(2L);
    }

    @Test
    void invalidCodingAnswerDoesNotCount() {
        var request = request("coding", "  ");
        assertThrows(RuntimeException.class, () -> service.submissionGradeStart(7L, request));
        verify(problemRepository, never()).incrementSubmittedCount(anyLong());
    }

    @Test
    void failedSubmissionSaveDoesNotCount() {
        var request = request("practice", "flag");
        when(submissionRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.submissionGradeStart(7L, request));
        verify(problemRepository, never()).incrementSubmittedCount(anyLong());
    }
}
