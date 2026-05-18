package net.diveon.backend.domain.contest.dto.request;

import jakarta.validation.constraints.NotBlank;

public class QnaCreateRequest {

    @NotBlank
    private String question;

    public QnaCreateRequest() {}

    public String getQuestion() { return question; }
}
