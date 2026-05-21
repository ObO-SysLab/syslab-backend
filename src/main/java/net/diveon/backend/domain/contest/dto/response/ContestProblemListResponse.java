package net.diveon.backend.domain.contest.dto.response;

import java.util.List;

import net.diveon.backend.domain.contest.others.ForDtoContestProblem;

public class ContestProblemListResponse {

    private List<ForDtoContestProblem> problems;

    public ContestProblemListResponse() {
    }

    public ContestProblemListResponse(List<ForDtoContestProblem> problems) {
        this.problems = problems;
    }

    public List<ForDtoContestProblem> getProblems() {
        return problems;
    }

    public void setProblems(List<ForDtoContestProblem> problems) {
        this.problems = problems;
    }
}
