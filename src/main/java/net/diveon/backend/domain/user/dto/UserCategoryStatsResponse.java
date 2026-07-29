package net.diveon.backend.domain.user.dto;

import java.util.Map;

public class UserCategoryStatsResponse {

    private static final int CATEGORY_MAX = 1080; // 오각형 차트 축 100% 기준값 (프론트에서 sqrt(score/categoryMax)로 사용)

    private final Map<String, Integer> categoryScores;
    private final int categoryMax;

    public UserCategoryStatsResponse(Map<String, Integer> categoryScores) {
        this.categoryScores = categoryScores;
        this.categoryMax = CATEGORY_MAX;
    }

    public Map<String, Integer> getCategoryScores() { return categoryScores; }
    public int getCategoryMax() { return categoryMax; }
}
