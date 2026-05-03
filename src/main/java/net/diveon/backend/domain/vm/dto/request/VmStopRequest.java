package net.diveon.backend.domain.vm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class VmStopRequest {

    @NotNull
    @JsonProperty("probId")
    private Long probId;

    public VmStopRequest() {}

    public Long getProbId() { return probId; }
}
