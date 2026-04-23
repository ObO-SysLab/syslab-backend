package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 코딩형 문제 수정 API의 응답 DTO
 * <pre>
 * {
 *   "status": 200,
 *   "message": "문제가 수정되었습니다.",
 *   "data": {
 *     "prob_id": 99,
 *     "type": "coding",
 *     "title": "프로세스 우선순위 정렬 심화",
 *     "updated_at": "2025-04-13T11:00:00Z"
 *   }
 * }
 * </pre>
 */
public class ProblemUpdateCodingResponse {

    @JsonProperty("prob_id")
    private Long probId;

    private String type;
    private String title;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ProblemUpdateCodingResponse() {
    }

    public ProblemUpdateCodingResponse(Long probId, String type, String title, String updatedAt) {
        this.probId = probId;
        this.type = type;
        this.title = title;
        this.updatedAt = updatedAt;
    }

    public Long getProbId() { return probId; }
    public void setProbId(Long probId) { this.probId = probId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
