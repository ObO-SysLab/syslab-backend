package net.diveon.backend.domain.contest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.contest.dto.request.ContestSubmitRequest;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.entity.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.service.CodingGradeQueueService;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContestSubmitAccessTest {
    @Mock ContestRepository contests;
    @Mock ContestProblemRepository contestProblems;
    @Mock ContestParticipantRepository participants;
    @Mock ContestSubmissionRepository contestSubmissions;
    @Mock UserRepository users;
    @Mock JdbcTemplate jdbc;
    @Mock ObjectMapper mapper;
    @Mock StringRedisTemplate redis;
    @Mock SolveSubmissionRepository submissions;
    @Mock SolveSubmissionCodingRepository codingSubmissions;
    @Mock CodingGradeQueueService queue;
    @Mock ProblemCodingRepository codingProblems;
    @Mock ProblemAccessService access;
    @Mock net.diveon.backend.domain.problem.repository.ProblemRepository problems;
    @InjectMocks ContestSubmitServiceImpl service;
    Contest contest;
    ContestParticipant participant;
    Problem problem;
    User user;

    @BeforeEach
    void setup() {
        contest = new Contest();
        ReflectionTestUtils.setField(contest, "id", 1L);
        contest.updateTime(java.time.LocalDateTime.now().minusDays(1), java.time.LocalDateTime.now().plusDays(1));
        user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        participant = new ContestParticipant(contest, user, ContestParticipant.ContestRole.PARTICIPANT);
        when(contests.findById(1L)).thenReturn(Optional.of(contest));
    }

    private ContestSubmitRequest request(String language, String code) {
        var request = new ContestSubmitRequest();
        ReflectionTestUtils.setField(request, "submissionType", ContestSubmitRequest.SubmissionType.CODING);
        ReflectionTestUtils.setField(request, "language", language);
        ReflectionTestUtils.setField(request, "code", code);
        return request;
    }

    private void registered(String type) {
        when(participants.findForScoreUpdate(1L, 7L)).thenReturn(Optional.of(participant));
        problem = new Problem(user, type, "fixture", "OS", "easy", "public");
        ReflectionTestUtils.setField(problem, "id", 2L);
        var cp = new ContestProblem(contest, problem, 100);
        ReflectionTestUtils.setField(cp, "id", 9L);
        when(contestProblems.findByIdAndContestId(9L, 1L)).thenReturn(Optional.of(cp));
        when(users.findById(7L)).thenReturn(Optional.of(user));
    }

    private void noEffects() {
        verifyNoInteractions(submissions, codingSubmissions, contestSubmissions, queue, jdbc, redis);
    }

    @Test void otherContestProblemIsRejectedBeforeSideEffects() {
        when(participants.findForScoreUpdate(1L, 7L)).thenReturn(Optional.of(participant));
        when(contestProblems.findByIdAndContestId(9L, 1L)).thenReturn(Optional.empty());
        assertThrows(ContestProblemNotFoundException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        verify(contestProblems, never()).findById(anyLong());
        noEffects();
    }

    @Test void nonparticipantIsRejected() {
        assertThrows(ContestAccessDeniedException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        verifyNoInteractions(contestProblems, users); noEffects();
    }

    @Test void bannedParticipantIsRejected() {
        participant.ban();
        when(participants.findForScoreUpdate(1L, 7L)).thenReturn(Optional.of(participant));
        assertThrows(ContestAccessDeniedException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        verifyNoInteractions(contestProblems, users); noEffects();
    }

    @Test void endedContestIsRejected() {
        contest.updateTime(java.time.LocalDateTime.now().minusDays(2), java.time.LocalDateTime.now().minusDays(1));
        assertThrows(ContestNotOngoingException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        verifyNoInteractions(participants, contestProblems); noEffects();
    }

    @ParameterizedTest @ValueSource(strings = {"objective", "practice"})
    void forgedTypeIsRejected(String actualType) {
        registered(actualType);
        assertThrows(InvalidSubmissionRequestException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        verifyNoInteractions(codingProblems); noEffects();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", "shell", "Python"})
    void unsupportedLanguageIsRejected(String language) {
        registered("coding");
        assertThrows(InvalidSubmissionRequestException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request(language, "print(1)")));
        verifyNoInteractions(codingProblems); noEffects();
    }

    @Test void languageDisallowedByProblemIsRejected() {
        registered("coding");
        var coding = new ProblemCoding(); coding.setAllowedLanguages(List.of("cpp"));
        when(codingProblems.findById(2L)).thenReturn(Optional.of(coding));
        assertThrows(InvalidSubmissionRequestException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)")));
        noEffects();
    }

    @Test void blankCodeIsRejected() {
        registered("coding");
        assertThrows(InvalidSubmissionRequestException.class,
                () -> service.submitContestProblem(1L, 9L, 7L, request("python", " ")));
        noEffects();
    }

    @Test void matchingCodingSubmissionKeepsBothIdsAndQueuesOnce() {
        registered("coding");
        var coding = new ProblemCoding(); coding.setAllowedLanguages(List.of("python"));
        when(codingProblems.findById(2L)).thenReturn(Optional.of(coding));
        when(submissions.save(any())).thenAnswer(call -> {
            SolveSubmission saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L); return saved;
        });
        when(contestSubmissions.save(any())).thenAnswer(call -> {
            ContestSubmission saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 202L); return saved;
        });
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var response = service.submitContestProblem(1L, 9L, 7L, request("python", "print(1)"));
        assertEquals(202, response.getStatusCode().value());
        verify(queue).sendContestGradeRequest(eq(2L), eq(7L), eq(101L), argThat(context ->
                context.getContestId().equals(1L) && context.getContestProblemId().equals(9L)));
        verify(submissions).save(any()); verify(contestSubmissions).save(any());
    }
}
