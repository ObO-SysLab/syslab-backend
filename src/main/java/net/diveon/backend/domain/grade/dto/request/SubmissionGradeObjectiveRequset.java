package net.diveon.backend.domain.grade.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.diveon.backend.domain.grade.dto.request.interfaces.SubmissionGradeRequest;

/**
 * <pre>
 * 다음의 요청과 매칭됨
 * {
  "prob_id": 42,
  "answer": [1, 3]
}
 * </pre>
 */
public class SubmissionGradeObjectiveRequset implements SubmissionGradeRequest<List<Integer>> {


    @JsonProperty("probId")
    private long ProbId;

    @JsonProperty("answer")
    private List<Integer> answer;

    @Override
    public long getProbId() {
        return ProbId;
    }
    @Override
    public List<Integer> getAnswer() {
        return answer;
    }
}
