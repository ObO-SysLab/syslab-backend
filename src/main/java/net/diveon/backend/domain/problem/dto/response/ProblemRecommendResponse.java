package net.diveon.backend.domain.problem.dto.response;

import java.util.List;

public class ProblemRecommendResponse {

    private final String reason;
    private final List<ProblemListItemResponse> problems;

    public ProblemRecommendResponse(String reason, List<ProblemListItemResponse> problems) {
        this.reason = reason;
        this.problems = problems;
    }

    public String getReason() { return reason; }
    public List<ProblemListItemResponse> getProblems() { return problems; }
}
