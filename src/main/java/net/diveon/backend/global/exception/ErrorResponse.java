package net.diveon.backend.global.exception;

public class ErrorResponse {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;

    public ErrorResponse(String type, String title, int status, String detail, String instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public int getStatus() { return status; }
    public String getDetail() { return detail; }
    public String getInstance() { return instance; }
}
