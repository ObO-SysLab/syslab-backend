package net.diveon.backend.domain.ad.dto.response;

import net.diveon.backend.domain.ad.entity.Ad;
import java.time.LocalDateTime;

public class AdResponse {
    private Long adId;
    private String imageUrl;
    private String linkUrl;
    private String altText;
    private LocalDateTime expiresAt;

    public AdResponse(Ad ad) {
        this.adId = ad.getId();
        this.imageUrl = ad.getImageUrl();
        this.linkUrl = ad.getLinkUrl();
        this.altText = ad.getAltText();
        this.expiresAt = ad.getExpiresAt();
    }

    public Long getAdId() { return adId; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public String getAltText() { return altText; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
