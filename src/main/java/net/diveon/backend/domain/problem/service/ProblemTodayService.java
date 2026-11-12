package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// 오늘의 다이브 포인트. 날짜(서울)별로 daily_problem에 기록해 두고 그 기록을 그날의 문제로 쓴다.
// 상점 포인트의 '오늘의 문제 정답 +50P'가 이 기록을 기준으로 지급되므로, 화면과 지급 기준이 항상 같다.
@Service
public class ProblemTodayService {

    static final String FIND_SQL = "SELECT prob_id FROM daily_problem WHERE day = ?";
    static final String INSERT_SQL = "INSERT INTO daily_problem (day, prob_id) VALUES (?, ?) ON CONFLICT (day) DO NOTHING";

    private final ProblemRepository problemRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;
    private final JdbcTemplate jdbc;

    public ProblemTodayService(ProblemRepository problemRepository,
                               SolveSubmissionRepository solveSubmissionRepository,
                               JdbcTemplate jdbc) {
        this.problemRepository = problemRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.jdbc = jdbc;
    }

    protected LocalDate today() {
        return LocalDate.now();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void refreshTodayProblem() {
        todayProblemId();
    }

    // 오늘 기록이 없으면 공개 문제 하나를 골라 기록한다. 동시에 두 번 골라도 먼저 기록된 문제를 쓴다.
    @Transactional
    public long todayProblemId() {
        Date day = Date.valueOf(today());
        List<Long> found = jdbc.query(FIND_SQL, (rs, i) -> rs.getLong(1), day);
        if (!found.isEmpty()) return found.get(0);
        Problem picked = problemRepository.findRandomPublicProblem().orElseThrow(ProblemNotFoundException::new);
        jdbc.update(INSERT_SQL, day, picked.getId());
        return jdbc.query(FIND_SQL, (rs, i) -> rs.getLong(1), day).get(0);
    }

    @Transactional
    public ProblemListItemResponse getTodayProblem(long userId) {
        Problem problem = problemRepository.findById(todayProblemId())
                .orElseThrow(ProblemNotFoundException::new);
        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));
        return ProblemListItemResponse.of(problem, solvedIds.contains(problem.getId()));
    }
}
