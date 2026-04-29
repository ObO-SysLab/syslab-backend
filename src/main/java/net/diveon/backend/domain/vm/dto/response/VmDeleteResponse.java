package net.diveon.backend.domain.vm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class VmDeleteResponse {

    @JsonProperty("container_id")
    private String containerId;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;

    public VmDeleteResponse(String containerId, LocalDateTime deletedAt) {
        this.containerId = containerId;
        this.deletedAt = deletedAt;
    }

    public String getContainerId() { return containerId; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}