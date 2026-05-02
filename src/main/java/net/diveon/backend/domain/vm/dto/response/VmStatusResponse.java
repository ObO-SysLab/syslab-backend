package net.diveon.backend.domain.vm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VmStatusResponse {

    private String status;

    @JsonProperty("container_id")
    private String containerId;

    private String message;

    public VmStatusResponse(String status, String containerId, String message) {
        this.status = status;
        this.containerId = containerId;
        this.message = message;
    }

    public static VmStatusResponse running(String containerId) {
        return new VmStatusResponse("RUNNING", containerId, "실행 중인 VM이 있습니다.");
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
}
