package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.user.dto.UserCategoryStatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCategoryStatsService {

    private static final List<String> ALL_CATEGORIES =
            List.of("Process", "Memory", "Kernel", "Thread", "FileSystem");

    private final SolveResultRepository solveResultRepository;

    public UserCategoryStatsService(SolveResultRepository solveResultRepository) {
        this.solveResultRepository = solveResultRepository;
    }

    @Transactional(readOnly = true)
    public UserCategoryStatsResponse getCategoryStats(Long userId) {
        List<Object[]> rows = solveResultRepository.findCategoryScoreByUserId(userId);

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String category : ALL_CATEGORIES) {
            scores.put(category, 0);
        }
        for (Object[] row : rows) {
            String category = (String) row[0];
            int score = ((Number) row[1]).intValue();
            scores.put(category, score);
        }

        return new UserCategoryStatsResponse(scores);
    }
}
