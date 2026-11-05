package net.diveon.backend.domain.vm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VmStatusResponse {

    private String status;

    @JsonProperty("containerId")
    private String containerId;

    private String message;
    private Long sessionId;
    private Long expiresAtEpochMs;

    public VmStatusResponse(String status, String containerId, String message) {
        this.status = status;
        this.containerId = containerId;
        this.message = message;
    }

    public static VmStatusResponse from(net.diveon.backend.domain.vm.entity.VmSession session) {
        var response = new VmStatusResponse(session.getStatus(), session.getContainerId(), "VM 상태를 확인했습니다.");
        response.sessionId = session.getId();
        if (session.getExpiresAt() != null) response.expiresAtEpochMs = session.getExpiresAt()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return response;
    }

    public static VmStatusResponse none() {
        return new VmStatusResponse("NONE", null, "실행 중인 VM이 없습니다.");
    }

    public static VmStatusResponse imageNotReady() {
        return new VmStatusResponse("IMAGE_NOT_READY", null, "이미지가 아직 준비 중입니다.");
    }

    public String getStatus() { return status; }
    public String getContainerId() { return containerId; }
    public String getMessage() { return message; }
    public Long getSessionId() { return sessionId; }
    public Long getExpiresAtEpochMs() { return expiresAtEpochMs; }
}
