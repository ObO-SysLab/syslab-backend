package net.diveon.backend.domain.vm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class VmCreateRequest {

    @NotNull
    @JsonProperty("prob_id")
    private Long probId;

    public VmCreateRequest() {}

    public Long getProbId() { return probId; }
}