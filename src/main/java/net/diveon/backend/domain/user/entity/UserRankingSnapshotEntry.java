package net.diveon.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "user_ranking_snapshot_entry",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_ranking_snapshot_entry_user", columnNames = {"snapshot_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_user_ranking_snapshot_entry_rank", columnList = "snapshot_id, rank, user_id"),
        @Index(name = "idx_user_ranking_snapshot_entry_user", columnList = "snapshot_id, user_id")
    }
)
public class UserRankingSnapshotEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "tier", nullable = false)
    private Integer tier;

    @Column(name = "score", nullable = false)
    private Integer score;

    public UserRankingSnapshotEntry() {}

    public Long getId() { return id; }
    public Long getSnapshotId() { return snapshotId; }
    public Long getUserId() { return userId; }
    public Integer getRank() { return rank; }
    public Integer getTier() { return tier; }
    public Integer getScore() { return score; }
}
