package net.diveon.backend.domain.contest.service;

import net.diveon.backend.domain.contest.dto.response.ContestRankingResponse;

public interface ContestRankingService {
    ContestRankingResponse getRankings(Long contestId, Long userId);
}
