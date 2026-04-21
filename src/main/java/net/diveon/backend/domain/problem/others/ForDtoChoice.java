package net.diveon.backend.domain.problem.others;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForDtoChoice {

    private Integer index;
    private String content;

    @JsonProperty("image_url")
    private String imageUrl;

    public ForDtoChoice() {
    }

    public ForDtoChoice(Integer index, String content, String imageUrl) {
        this.index = index;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
