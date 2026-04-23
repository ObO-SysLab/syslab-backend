package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.others.ForDtoChoice;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;

import java.util.List;

/**
 * 객관식 문제 상세 조회 응답의 data 영역을 위한 DTO
 *
 * <pre>
 * {
 *   "status": 200,
 *   "message": "객관식 문제 상세 조회에 성공하였습니다.",
 *   "data": {
 *     "prob_id": 42,
 *     "author": "박단용",
 *     "type": "objective",
 *     "title": "프로세스 스케줄링 이해하기",
 *     "category": "process",
 *     "difficulty": "medium",
 *     "visibility": "public",
 *     "summary": "Round Robin 스케줄링 개념 이해 문제",
 *     "description": "다음 중 Round Robin 스케줄링에 대한 설명으로 옳은 것은?",
 *     "choices": [
 *       { "index": 1, "content": "선점형이다", "image_url": null }
 *     ],
 *     "answer": 1,
 *     "obo_enabled": true,
 *     "obo_steps": [
 *       { "step": 1, "description": "설명", "image_url": null }
 *     ],
 *     "solved_count": 317,
 *     "submitted_count": 412,
 *     "created_at": "2025-03-01T09:00:00",
 *     "updated_at": "2025-03-10T14:30:00"
 *   }
 * }
 * </pre>
 */
public class ProblemDetailObjectiveResponse implements ProblemDetailResponse {

    @JsonProperty("prob_id")
    private Long probId;

    private String author;
    private String type;
    private String title;
    private String category;
    private String difficulty;
    private String visibility;
    private String summary;
    private String description;
    private List<ForDtoChoice> choices;
    private Integer answer;

    @JsonProperty("obo_enabled")
    private Boolean oboEnabled;

    @JsonProperty("obo_steps")
    private List<ForDtoOboStep> oboSteps;

    @JsonProperty("solved_count")
    private Integer solvedCount;

    @JsonProperty("submitted_count")
    private Integer submittedCount;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ProblemDetailObjectiveResponse() {
    }

    public ProblemDetailObjectiveResponse(
        Long probId,
        String author,
        String type,
        String title,
        String category,
        String difficulty,
        String visibility,
        String summary,
        String description,
        List<ForDtoChoice> choices,
        Integer answer,
        Boolean oboEnabled,
        List<ForDtoOboStep> oboSteps,
        Integer solvedCount,
        Integer submittedCount,
        String createdAt,
        String updatedAt
    ) {
        this.probId = probId;
        this.author = author;
        this.type = type;
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.summary = summary;
        this.description = description;
        this.choices = choices;
        this.answer = answer;
        this.oboEnabled = oboEnabled;
        this.oboSteps = oboSteps;
        this.solvedCount = solvedCount;
        this.submittedCount = submittedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /*
     * 현재는 DTO 내부의 정적 팩토리 메서드(of)를 통해
     * Problem, ProblemObjective, OboStep 엔티티를 응답 DTO로 조립합니다.
     *
     * 이 방식은 서비스 코드의 길이를 줄이고, DTO 생성 규칙을 한 곳에 모을 수 있다는 장점이 있습니다.
     *
     * 다만 추후 상세 조회 응답 종류가 늘어나거나,
     * objective/practice/coding 간 변환 로직이 복잡해질 경우
     * DTO 내부에서 변환을 담당하기보다 별도의 Mapper 클래스로 분리할 수 있습니다.
     *
     * 예시:
     * ProblemDetailMapper.toObjectiveResponse(problem, problemObjective, oboSteps)
     *
     * Mapper로 분리하면 DTO는 단순 데이터 전달 객체 역할에 집중하고,
     * 엔티티 -> 응답 DTO 변환 책임은 Mapper가 담당하게 됩니다.
     */
    public static ProblemDetailObjectiveResponse of(
        Problem problem,
        ProblemObjective problemObjective,
        List<OboStep> oboSteps
    ) {
        List<ForDtoOboStep> oboStepResponses = oboSteps.stream()
            .map(oboStep -> new ForDtoOboStep(
                oboStep.getStep(),
                oboStep.getDescription(),
                oboStep.getImageUrl()
            ))
            .toList();

        return new ProblemDetailObjectiveResponse(
            problem.getId(),
            problem.getAuthor().getNickname(),
            problem.getType(),
            problem.getTitle(),
            problem.getCategory(),
            problem.getDifficulty(),
            problem.getVisibility(),
            problemObjective.getSummary(),
            problemObjective.getDescription(),
            problemObjective.getChoices(),
            problemObjective.getAnswer(),
            problemObjective.getOboEnabled(),
            oboStepResponses,
            problem.getSolvedCount(),
            problem.getSubmittedCount(),
            problem.getCreatedAt().toString(),
            problem.getUpatedAt().toString()
        );
    }

    public Long getProbId() {
        return probId;
    }

    public String getAuthor() {
        return author;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public List<ForDtoChoice> getChoices() {
        return choices;
    }

    public Integer getAnswer() {
        return answer;
    }

    public Boolean getOboEnabled() {
        return oboEnabled;
    }

    public List<ForDtoOboStep> getOboSteps() {
        return oboSteps;
    }

    public Integer getSolvedCount() {
        return solvedCount;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
