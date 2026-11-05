package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public class ProblemUpdateObjectiveRequest {

    private String title;
    private String expectedEditVersion;
    public String getExpectedEditVersion(){return expectedEditVersion;}
    public void setExpectedEditVersion(String value){expectedEditVersion=value;}
    private String summary;
    private String description;
    private String difficulty;
    private JsonNode oboJson;
    private String oboAction;
    private java.util.List<net.diveon.backend.domain.problem.others.ForDtoChoice> choices;
    private java.util.List<Integer> answer;
    public java.util.List<net.diveon.backend.domain.problem.others.ForDtoChoice> getChoices(){return choices;}
    public void setChoices(java.util.List<net.diveon.backend.domain.problem.others.ForDtoChoice> choices){this.choices=choices;}
    public java.util.List<Integer> getAnswer(){return answer;}
    public void setAnswer(java.util.List<Integer> answer){this.answer=answer;}
    public String getOboAction(){return oboAction;}
    public void setOboAction(String oboAction){this.oboAction=oboAction;}

    public ProblemUpdateObjectiveRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public JsonNode getOboJson() { return oboJson; }
    public void setOboJson(JsonNode oboJson) { this.oboJson = oboJson; }
}
