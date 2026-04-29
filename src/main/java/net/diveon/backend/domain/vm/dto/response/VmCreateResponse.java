package net.diveon.backend.domain.vm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.vm.entity.VmContainer;

import java.time.LocalDateTime;

public class VmCreateResponse {

    @JsonProperty("container_id")
    private String containerId;

    @JsonProperty("prob_id")
    private Long probId;

    private String image;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    public VmCreateResponse(String containerId, Long probId, String image,
                            LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.containerId = containerId;
        this.probId = probId;
        this.image = image;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static VmCreateResponse from(VmContainer vm) {
        return new VmCreateResponse(
                vm.getContainerId(),
                vm.getProbId(),
                vm.getImage(),
                vm.getCreatedAt(),
                vm.getExpiresAt()
        );
    }

    public String getContainerId() { return containerId; }
    public Long getProbId() { return probId; }
    public String getImage() { return image; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}