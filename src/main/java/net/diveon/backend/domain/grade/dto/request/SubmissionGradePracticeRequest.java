package net.diveon.backend.domain.grade.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.diveon.backend.domain.grade.dto.request.interfaces.SubmissionGradeRequest;

public class SubmissionGradePracticeRequest implements SubmissionGradeRequest<String>{
    

    @JsonProperty("probId")
    private long probId;


    @JsonProperty("answer")
    private String answer;


    public SubmissionGradePracticeRequest() {
        
    }

    @Override
    public String getAnswer() {
        return answer;
    }

    @Override 
    public long getProbId(){
        return probId;
    }
}
