package net.diveon.backend.domain.group.dto;

public class GroupAssignDecisionRequest {

    private String decidedReason;

    public GroupAssignDecisionRequest() {
    }

    public String getDecidedReason() { return decidedReason; }
    public void setDecidedReason(String decidedReason) { this.decidedReason = decidedReason; }
}
