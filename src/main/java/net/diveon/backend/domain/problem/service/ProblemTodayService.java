package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ProblemTodayService {

    private static final String TODAY_PROBLEM_KEY = "today_problem_id";

    private final ProblemRepository problemRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ProblemTodayService(ProblemRepository problemRepository,
                               SolveSubmissionRepository solveSubmissionRepository,
                               RedisTemplate<String, String> redisTemplate) {
        this.problemRepository = problemRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshTodayProblem() {
        problemRepository.findRandomPublicProblem().ifPresent(problem ->
                redisTemplate.opsForValue().set(TODAY_PROBLEM_KEY, String.valueOf(problem.getId()), 25, TimeUnit.HOURS)
        );
    }

    @Transactional(readOnly = true)
    public ProblemListItemResponse getTodayProblem(long userId) {
        String problemIdStr = redisTemplate.opsForValue().get(TODAY_PROBLEM_KEY);

        Problem problem;
        if (problemIdStr == null) {
            problem = problemRepository.findRandomPublicProblem()
                    .orElseThrow(ProblemNotFoundException::new);
            redisTemplate.opsForValue().set(TODAY_PROBLEM_KEY, String.valueOf(problem.getId()), 25, TimeUnit.HOURS);
        } else {
            problem = problemRepository.findById(Long.parseLong(problemIdStr))
                    .orElseThrow(ProblemNotFoundException::new);
        }

        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));
        return ProblemListItemResponse.of(problem, solvedIds.contains(problem.getId()));
    }
}
