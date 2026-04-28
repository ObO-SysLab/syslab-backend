
package net.diveon.backend.domain.problem.dto.request;

import net.diveon.backend.domain.problem.others.ForDtoChoice;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;

import java.util.List;

/**
 * 다음과 같은 형태의 Patch request 양식을 만족하기 위한 dto
 * <pre>
 * {
  "title": {{title}},
  "description": {{description}},
  "category": {{category}},
  "difficulty": {{medium}},
  "visibility": "{{visibility}}", // 여기까지 String
  "summary": "{{summary}}",
  "choices": [
    { "index": 1, "content": "선점형이다", "image_url": null },
    { "index": 2, "content": "비선점형이다", "image_url": null },
    { "index": 3, "content": "우선순위 기반이다", "image_url": null },
    { "index": 4, "content": "FIFO 방식이다", "image_url": null }
  ],  // JSONB로 데이터베이스 저장될 것임.
  "answer": [1, 2],
  "oboEnabled": {{enabled}},
  "obo": {  // object 타입
    "steps": [
      {
        "step": 1,
        "description": "프로세스 A가 CPU를 점유합니다.",
        "image_url": "https://cdn.dk-world.com/problems/p42_obo_step1.png"
      },
      {
        "step": 2,
        "description": "타임 퀀텀이 만료되어 프로세스 B로 전환됩니다.",
        "image_url": "https://cdn.dk-world.com/problems/p42_obo_step2.png"
      }
    ]
  }
}
 * </pre>
 */

public class ProblemUpdateObjectiveRequest {

    private String title;
    private String description;
    private String category;
    private String difficulty;
    private String visibility;
    private String summary;
    private List<ForDtoChoice> choices;
    private List<Integer> answer;
    private Boolean oboEnabled;
    private Obo obo;

    public ProblemUpdateObjectiveRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ForDtoChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ForDtoChoice> choices) {
        this.choices = choices;
    }

    public List<Integer> getAnswer() {
        return answer;
    }

    public void setAnswer(List<Integer> answer) {
        this.answer = answer;
    }

    public Boolean getOboEnabled() {
        return oboEnabled;
    }

    public void setOboEnabled(Boolean oboEnabled) {
        this.oboEnabled = oboEnabled;
    }

    public Obo getObo() {
        return obo;
    }

    public void setObo(Obo obo) {
        this.obo = obo;
    }

    public static class Obo {
        private List<ForDtoOboStep> steps;

        public Obo() {
        }

        public List<ForDtoOboStep> getSteps() {
            return steps;
        }

        public void setSteps(List<ForDtoOboStep> steps) {
            this.steps = steps;
        }
    }
}
