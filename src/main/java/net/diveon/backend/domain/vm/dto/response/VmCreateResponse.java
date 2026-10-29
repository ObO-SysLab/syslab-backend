package net.diveon.backend.domain.vm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.vm.entity.VmSession;

import java.time.LocalDateTime;

public class VmCreateResponse {

    private String status;

    @JsonProperty("containerId")
    private String containerId;

    private String message;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;
    private Long sessionId;
    private Long expiresAtEpochMs;

    public VmCreateResponse(String status, String containerId, String message, LocalDateTime expiresAt) {
        this.status = status;
        this.containerId = containerId;
        this.message = message;
        this.expiresAt = expiresAt;
    }

    public static VmCreateResponse from(VmSession session, String message) {
        var response = new VmCreateResponse("success", session.getContainerId(), message, session.getExpiresAt());
        response.sessionId = session.getId();
        response.expiresAtEpochMs = session.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return response;
    }

    public String getStatus() { return status; }
    public String getContainerId() { return containerId; }
    public String getMessage() { return message; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Long getSessionId() { return sessionId; }
    public Long getExpiresAtEpochMs() { return expiresAtEpochMs; }
}
