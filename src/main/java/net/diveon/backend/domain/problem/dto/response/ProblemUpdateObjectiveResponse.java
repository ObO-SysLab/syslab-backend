package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * <pre>
 * {
    "status": 200,
    "message": "문제가 수정되었습니다.",
    "data": {
        "probId": 42,
        "type": "objective",
        "title": "프로세스 스케줄링 심화",
        "updatedAt": "2025-04-13T11:00:00Z"
    }
}
  </pre>
 */
public class ProblemUpdateObjectiveResponse {
    @JsonProperty("probId")
    private long probId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public ProblemUpdateObjectiveResponse(){}
    
    //이 구현방식은 민준형처럼, 해당하는 상위 entity를 가져와서 사용하는것도 좋을 듯.
    public ProblemUpdateObjectiveResponse(long id, String type, String title, String updatedAt){
      this.probId = id;
      this.type = type;
      this.title = title;
      this.updatedAt = updatedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
    public long getProbId() {
        return probId;
    }
    public String getTitle() {
        return title;
    }
    public String getType() {
        return type;
    }
}
