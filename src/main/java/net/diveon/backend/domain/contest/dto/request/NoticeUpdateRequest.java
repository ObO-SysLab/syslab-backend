package net.diveon.backend.domain.contest.dto.request;

import jakarta.validation.constraints.NotBlank;

public class NoticeUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    public String getTitle() { return title; }
    public String getContent() { return content; }
}
