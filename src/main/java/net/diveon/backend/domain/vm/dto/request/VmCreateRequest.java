package net.diveon.backend.domain.vm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class VmCreateRequest {

    @NotNull
    @JsonProperty("probId")
    private Long probId;

    public VmCreateRequest() {}

    public Long getProbId() { return probId; }
    public void setProbId(Long probId) { this.probId = probId; }
}