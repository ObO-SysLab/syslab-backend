package net.diveon.backend.domain.contest.service;

import org.springframework.http.ResponseEntity;
import net.diveon.backend.domain.contest.dto.request.ContestSubmitRequest;
import net.diveon.backend.global.response.ApiResponse;

public interface ContestSubmitService {
    ResponseEntity<ApiResponse<?>> submitContestProblem(Long contestId, Long contestProblemId, Long userId, ContestSubmitRequest request);
}