package net.diveon.backend.domain.user.dto;

import java.util.Map;

public class UserCategoryStatsResponse {

    private final Map<String, Integer> categoryScores;

    public UserCategoryStatsResponse(Map<String, Integer> categoryScores) {
        this.categoryScores = categoryScores;
    }

    public Map<String, Integer> getCategoryScores() { return categoryScores; }
}
