package net.diveon.backend.domain.problem.dto.response;

public class ProblemDeleteResponse {
    private long probId;

    public ProblemDeleteResponse(){}

    public ProblemDeleteResponse(long prod_id){
        this.probId = prod_id;
    }

    public long getProbId() {
        return probId;
    }
}
