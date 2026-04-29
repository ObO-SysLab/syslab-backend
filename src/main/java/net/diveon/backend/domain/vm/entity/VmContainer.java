package net.diveon.backend.domain.vm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vm_container")
public class VmContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "container_id", nullable = false, length = 64)
    private String containerId;

    @Column(name = "prob_id", nullable = false)
    private Long probId;

    @Column(name = "image", nullable = false, length = 200)
    private String image;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // JPA용 기본 생성자
    public VmContainer() {}

    public VmContainer(Long userId, String containerId, Long probId, String image,
                       LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.userId = userId;
        this.containerId = containerId;
        this.probId = probId;
        this.image = image;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getContainerId() { return containerId; }
    public Long getProbId() { return probId; }
    public String getImage() { return image; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
