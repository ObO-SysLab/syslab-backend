package net.diveon.backend.domain.problem.others;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OboStep {

    private Integer step;
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    public OboStep() {
    }

    public OboStep(Integer step, String description, String imageUrl) {
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
