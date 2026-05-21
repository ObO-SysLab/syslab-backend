package net.diveon.backend.domain.contest.dto.request;

import jakarta.validation.constraints.NotBlank;

public class QnaAnswerRequest {

    @NotBlank
    private String answer;

    public QnaAnswerRequest() {}

    public String getAnswer() { return answer; }
}
