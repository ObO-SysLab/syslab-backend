package net.diveon.backend.domain.problem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public class ProblemUpdateCodingRequest {

    private String title;
    private String summary;
    private String description;
    private String difficulty;
    private JsonNode oboJson;
    private String oboAction;
    private ProblemCreateCodingRequest.Constraints constraints;
    private java.util.List<net.diveon.backend.domain.problem.others.ForDtoTestCase> testcases;
    private String inputDescription,outputDescription;
    public ProblemCreateCodingRequest.Constraints getConstraints(){return constraints;}
    public void setConstraints(ProblemCreateCodingRequest.Constraints constraints){this.constraints=constraints;}
    public java.util.List<net.diveon.backend.domain.problem.others.ForDtoTestCase> getTestcases(){return testcases;}
    public void setTestcases(java.util.List<net.diveon.backend.domain.problem.others.ForDtoTestCase> testcases){this.testcases=testcases;}
    public String getInputDescription(){return inputDescription;}
    public void setInputDescription(String value){inputDescription=value;}
    public String getOutputDescription(){return outputDescription;}
    public void setOutputDescription(String value){outputDescription=value;}
    public String getOboAction(){return oboAction;}
    public void setOboAction(String oboAction){this.oboAction=oboAction;}

    public ProblemUpdateCodingRequest() {}

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
