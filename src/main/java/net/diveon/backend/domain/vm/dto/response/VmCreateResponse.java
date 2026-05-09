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

    public VmCreateResponse(String status, String containerId, String message, LocalDateTime expiresAt) {
        this.status = status;
        this.containerId = containerId;
        this.message = message;
        this.expiresAt = expiresAt;
    }

    public static VmCreateResponse from(VmSession session, String message) {
        return new VmCreateResponse("success", session.getContainerId(), message, session.getExpiresAt());
    }

    public String getStatus() { return status; }
    public String getContainerId() { return containerId; }
    public String getMessage() { return message; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
