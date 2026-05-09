package net.diveon.backend.domain.problem.others;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForDtoTestCase {

    private Integer index;
    private String input;
    private String output;

    @JsonProperty("isSample")
    private Boolean isSample;

    // 기본 생성자
    public ForDtoTestCase() {
    }

    // 전체 생성자
    public ForDtoTestCase(Integer index, String input, String output, Boolean isSample) {
        this.index = index;
        this.input = input;
        this.output = output;
        this.isSample = isSample;
    }

    // Getter 메서드
    public Integer getIndex() { return index; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public Boolean getIsSample() { return isSample; }

    // Setter 메서드
    public void setIndex(Integer index) { this.index = index; }
    public void setInput(String input) { this.input = input; }
    public void setOutput(String output) { this.output = output; }
    public void setIsSample(Boolean isSample) { this.isSample = isSample; }
}
