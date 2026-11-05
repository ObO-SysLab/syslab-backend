package net.diveon.backend.domain.vm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class VmStopRequest {

    @NotNull
    @JsonProperty("probId")
    private Long probId;

    @NotNull @Positive
    private Long sessionId;

    public VmStopRequest() {}

    public Long getProbId() { return probId; }
    public Long getSessionId() { return sessionId; }
}
