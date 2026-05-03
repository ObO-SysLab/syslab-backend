package net.diveon.backend.domain.vm.dto.response;

public class VmStopResponse {

    private String status;
    private String message;

    public VmStopResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
