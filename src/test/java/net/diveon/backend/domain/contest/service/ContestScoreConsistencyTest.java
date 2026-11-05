package net.diveon.backend.domain.contest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.contest.dto.request.ContestSubmitRequest;
import net.diveon.backend.domain.contest.entity.*;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.grade.service.CodingGradeQueueService;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContestScoreConsistencyTest {
    final ContestRepository contests = mock(ContestRepository.class);
    final ContestProblemRepository contestProblems = mock(ContestProblemRepository.class);
    final ContestParticipantRepository participants = mock(ContestParticipantRepository.class);
    final ContestSubmissionRepository submissions = mock(ContestSubmissionRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final ProblemRepository problems = mock(ProblemRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final ContestSubmitServiceImpl service = new ContestSubmitServiceImpl(contests, contestProblems, participants,
            submissions, users, jdbc, new ObjectMapper(), redis, mock(SolveSubmissionRepository.class),
            mock(SolveSubmissionCodingRepository.class), mock(CodingGradeQueueService.class),
            mock(ProblemCodingRepository.class), mock(ProblemAccessService.class), problems,
            mock(net.diveon.backend.domain.problem.service.ProblemRevisionService.class));
    final LocalDateTime solvedAt = LocalDateTime.of(2026, 10, 30, 0, 0);
    ContestParticipant participant;

    ContestSubmitRequest setup(String type, boolean correct) throws Exception {
        var user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        var contest = new Contest(); ReflectionTestUtils.setField(contest, "id", 1L);
        contest.updateTime(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        participant = new ContestParticipant(contest, user, ContestParticipant.ContestRole.PARTICIPANT);
        var problem = new Problem(user, type, "fixture", "OS", "easy", "public");
        ReflectionTestUtils.setField(problem, "id", 2L);
        when(problems.findForRevision(2L)).thenReturn(Optional.of(problem));
        var cp = new ContestProblem(contest, problem, 100); ReflectionTestUtils.setField(cp, "id", 9L);
        when(contests.findById(1L)).thenReturn(Optional.of(contest));
        when(participants.findForScoreUpdate(1L, 7L)).thenReturn(Optional.of(participant));
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.of(participant));
        when(contestProblems.findByIdAndContestId(9L, 1L)).thenReturn(Optional.of(cp));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(jdbc.queryForMap(anyString(), eq(2L))).thenReturn(Map.of("answer", "[1]", "is_seqeuntial", false));
        when(jdbc.queryForObject(anyString(), eq(String.class), eq(2L)))
                .thenReturn(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("flag".getBytes())));
        when(jdbc.queryForObject(anyString(), eq(LocalDateTime.class))).thenReturn(solvedAt);
        when(jdbc.queryForObject(ContestRankQuery.SQL, Integer.class, 1L, 7L)).thenReturn(2);
        when(submissions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        var request = new ContestSubmitRequest();
        ReflectionTestUtils.setField(request, "submissionType", ContestSubmitRequest.SubmissionType.valueOf(type.toUpperCase()));
        ReflectionTestUtils.setField(request, "answer", type.equals("objective") ? (correct ? "[1]" : "[2]") : (correct ? "flag" : "wrong"));
        return request;
    }

    @ParameterizedTest @ValueSource(strings={"objective", "practice"})
    void lockPrecedesFirstSolveDecisionAndFlushPrecedesRank(String type) throws Exception {
        var request = setup(type, true);
        service.submitContestProblem(1L, 9L, 7L, request);
        assertThat(participant.getScore()).isEqualTo(100);
        assertThat(participant.getLastSolvedAt()).isEqualTo(solvedAt);
        var order = inOrder(participants, submissions, jdbc, problems);
        order.verify(participants).findForScoreUpdate(1L, 7L);
        order.verify(submissions).findSolvedContestProblemIds(1L, 7L);
        order.verify(participants).saveAndFlush(participant);
        order.verify(problems).recordSolver(2L, 7L);
        order.verify(jdbc).queryForObject(ContestRankQuery.SQL, Integer.class, 1L, 7L);
        verify(redis, never()).opsForZSet();
    }

    @ParameterizedTest @ValueSource(strings={"objective", "practice"})
    void repeatCorrectDoesNotAwardPointsAgainButUsesGlobalDeduplication(String type) throws Exception {
        var request = setup(type, true);
        when(submissions.findSolvedContestProblemIds(1L, 7L)).thenReturn(List.of(9L));
        participant.addScore(100, solvedAt);
        service.submitContestProblem(1L, 9L, 7L, request);
        assertThat(participant.getScore()).isEqualTo(100);
        verify(participants, never()).saveAndFlush(any());
        verify(problems).recordSolver(2L, 7L);
    }

    @ParameterizedTest @ValueSource(strings={"objective", "practice"})
    void wrongAnswerChangesNeitherScoreNorSolverCount(String type) throws Exception {
        var request = setup(type, false);
        service.submitContestProblem(1L, 9L, 7L, request);
        assertThat(participant.getScore()).isZero();
        verify(problems, never()).recordSolver(anyLong(), anyLong());
        verify(participants, never()).saveAndFlush(any());
    }

    @Test void rankingUsesDatabaseEvenWhenRedisContainsOlderOrder() {
        var contest = new Contest(); contest.updateTime(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        var user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        var p = new ContestParticipant(contest, user, ContestParticipant.ContestRole.PARTICIPANT); p.addScore(200, solvedAt);
        when(contests.findById(1L)).thenReturn(Optional.of(contest));
        when(participants.findByContestIdAndUserId(1L, 7L)).thenReturn(Optional.of(p));
        when(participants.findRankingsByContestId(1L)).thenReturn(List.of(p));
        when(submissions.countSolvedByUserForContest(1L)).thenReturn(Collections.singletonList(new Object[]{7L, 2L}));
        var result = new ContestRankingServiceImpl(contests, participants, submissions, redis).getRankings(1L, 7L);
        assertThat(result.getRankings().getFirst().getScore()).isEqualTo(200);
        assertThat(result.getRankings().getFirst().getSolvedCount()).isEqualTo(2);
        verifyNoInteractions(redis);
    }
}
