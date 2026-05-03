package net.diveon.backend.domain.problem.others;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForDtoOboStep {

    private Integer step;
    private String description;

    @JsonProperty("imageUrl")
    private String imageUrl;

    public ForDtoOboStep() {
    }

    public ForDtoOboStep(Integer step, String description, String imageUrl) {
        this.step = step;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
