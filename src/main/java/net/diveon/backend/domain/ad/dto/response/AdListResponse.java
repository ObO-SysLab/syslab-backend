package net.diveon.backend.domain.ad.dto.response;

import java.util.List;

public class AdListResponse {
    private String placement;
    private int width;
    private int height;
    private List<AdResponse> ads;

    public AdListResponse(String placement, int width, int height, List<AdResponse> ads) {
        this.placement = placement;
        this.width = width;
        this.height = height;
        this.ads = ads;
    }

    public String getPlacement() { return placement; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<AdResponse> getAds() { return ads; }
}
