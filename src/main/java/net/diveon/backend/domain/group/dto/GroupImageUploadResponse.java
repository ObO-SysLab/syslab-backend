package net.diveon.backend.domain.group.dto;

public class GroupImageUploadResponse {

    private final String imageUrl;

    public GroupImageUploadResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
