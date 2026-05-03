package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 해당 dto는 아래와 같은 형태의 응답을 위해서 필요함. 
 * 다만 
 *   "status": 201,
  "message": "문제가 등록되었습니다.",
  의 부분은 사전 정의된 틀이 존재하고
  실제로는 "data": {}의 내용만 구현이 필요하다.
 * 
  {
  "status": 201,
  "message": "문제가 등록되었습니다.",
  "data": {
    "probId": 42,
    "type": "objective",
    "title": "프로세스 스케줄링 이해하기",
    "createdAt": "2025-04-13T10:00:00Z"
    }
  }
 */
public class ProblemCreateObjectiveResponse {
    @JsonProperty("probId")
    private long probId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("createdAt")
    private String createdAt;

    public ProblemCreateObjectiveResponse(){}
    
    //이 구현방식은 민준형처럼, 해당하는 상위 entity를 가져와서 사용하는것도 좋을 듯.
    public ProblemCreateObjectiveResponse(long id, String type, String title, String createdAt){
      this.probId = id;
      this.type = type;
      this.title = title;
      this.createdAt = createdAt;
    }

    public String getCreatedAt() {
        return createdAt;
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
