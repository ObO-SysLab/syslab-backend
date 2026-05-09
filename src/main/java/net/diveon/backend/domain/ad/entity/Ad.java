package net.diveon.backend.domain.ad.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "ad")
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "link_url", nullable = false, columnDefinition = "TEXT")
    private String linkUrl;

    @Column(name = "alt_text", length = 255, nullable = false)
    private String altText;

    @Column(name = "placement", length = 50, nullable = false)
    private String placement; // main, probList, probDetail

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor
    public Ad() {}

    public Ad(String imageUrl, String linkUrl, String altText, String placement,
              int width, int height, LocalDateTime expiresAt) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.altText = altText;
        this.placement = placement;
        this.width = width;
        this.height = height;
        this.expiresAt = expiresAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public String getAltText() { return altText; }
    public String getPlacement() { return placement; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public void setAltText(String altText) { this.altText = altText; }
    public void setPlacement(String placement) { this.placement = placement; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
