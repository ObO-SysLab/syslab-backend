package net.diveon.backend.domain.grade.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.common.lang.NonNull;

public class SubmissionGradeReqeust {
    
    @NonNull
    @JsonProperty("probId")
    private long prodId;

    @NonNull
    @JsonProperty("answer")
    private Object answer;

    @JsonProperty("language")
    private String language;

    public SubmissionGradeReqeust() {}

    public long getProdId() {
        return prodId;
    }

    public Object getAnswer() {
        return answer;
    }

    public String getLanguage() {
        return language;
    }
}
