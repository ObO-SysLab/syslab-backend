package net.diveon.backend.domain.user.dto;

public class ProfileImageUploadResponse {

    private final String imageUrl;

    public ProfileImageUploadResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
