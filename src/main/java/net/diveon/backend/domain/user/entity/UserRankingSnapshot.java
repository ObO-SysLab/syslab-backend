package net.diveon.backend.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "user_ranking_snapshot",
    indexes = {
        @Index(name = "idx_user_ranking_snapshot_status_calculated", columnList = "status, calculated_at")
    }
)
public class UserRankingSnapshot {

    public static final String STATUS_BUILDING = "BUILDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    public UserRankingSnapshot() {}

    public UserRankingSnapshot(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
        this.status = STATUS_BUILDING;
    }

    public void complete() {
        this.status = STATUS_COMPLETED;
    }

    public Long getId() { return id; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public String getStatus() { return status; }
}
