package net.diveon.backend.domain.group.dto;

public class GroupAddProblemsRequest {

    private Long problemId;

    public GroupAddProblemsRequest() {
    }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
}
