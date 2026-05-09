package net.diveon.backend.domain.group.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.diveon.backend.domain.user.entity.User;

@Entity
@Table(name = "group_assign_request")
public class GroupAssignRequest {

    public static enum AssignRequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AssignRequestStatus status;

    @Column(name = "applied_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime appliedAt;

    @Column(name = "decided_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "decided_reason", length = 255)
    private String decidedReason;

    public GroupAssignRequest() {
    }

    public GroupAssignRequest(Group group, User user) {
        this.group = group;
        this.user = user;
        this.status = AssignRequestStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public User getUser() { return user; }
    public AssignRequestStatus getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public User getDecidedBy() { return decidedBy; }
    public String getDecidedReason() { return decidedReason; }
}
