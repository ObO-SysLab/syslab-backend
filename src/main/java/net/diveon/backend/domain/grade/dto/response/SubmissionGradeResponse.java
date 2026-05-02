package net.diveon.backend.domain.grade.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.common.lang.NonNull;

/**
 * <pre>
 * 다음의 형식으로 공통 응답이 반환됨
 * {
  "status": 202,
  "message": "채점 요청이 접수되었습니다.",
  "data": {
    "submission_id": "17",
    "prob_id": 42,
    "submission_status": "PENDING"
  }
}
기본 공통 데이터(상태코드, 메세지)
제출물 고유번호, 문제의 고유번호, 현재 상태
 * </pre>
 */
public class SubmissionGradeResponse {

    @NonNull
    @JsonProperty("submissionId")
    private long submissionId;

    @NonNull
    @JsonProperty("probId")
    private long probId;

    @NonNull
    @JsonProperty("submissionStatus")
    private String submissionStatus;

    //생성자
    public SubmissionGradeResponse() {}

    public SubmissionGradeResponse(long submissionId, 
        long probId, 
        String submissionStatus
    ){
        this.submissionId = submissionId;
        this.probId = probId;
        this.submissionStatus = submissionStatus;
    }

    //getter
    public long getProbId() {
        return probId;
    }
    public long getSubmissionId() {
        return submissionId;
    }
    public String getSubmissionStatus() {
        return submissionStatus;
    }


}
