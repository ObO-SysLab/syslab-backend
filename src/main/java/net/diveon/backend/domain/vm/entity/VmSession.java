package net.diveon.backend.domain.vm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vm_sessions")
public class VmSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "prob_id", nullable = false)
    private Long probId;

    @Column(name = "container_id", length = 100)
    private String containerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "runtime_version", nullable = false)
    private int runtimeVersion;

    public VmSession() {}

    public VmSession(Long userId, Long probId, String containerId,
                     LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.userId = userId;
        this.probId = probId;
        this.containerId = containerId;
        this.status = "RUNNING";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProbId() { return probId; }
    public String getContainerId() { return containerId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getRuntimeVersion() { return runtimeVersion; }

    public static VmSession creating(Long userId, Long probId, LocalDateTime now) {
        VmSession session = new VmSession(userId, probId, null, now, now.plusHours(2));
        session.status = "CREATING";
        session.runtimeVersion = 1;
        return session;
    }

    public void running(String containerId) { this.containerId = containerId; this.status = "RUNNING"; }
    public void stopping() { this.status = "STOPPING"; }

    public void stop() { this.status = "STOPPED"; }
}
