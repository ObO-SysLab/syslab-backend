package net.diveon.backend.domain.problem.dto.response;

public class ProblemDeleteResponse {
    private long probId;

    public ProblemDeleteResponse(){}

    public ProblemDeleteResponse(long probId){
        this.probId = probId;
    }

    public long getProbId() {
        return probId;
    }
}
