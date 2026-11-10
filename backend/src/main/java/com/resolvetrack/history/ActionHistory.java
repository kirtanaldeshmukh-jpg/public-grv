package com.resolvetrack.history;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_history")
public class ActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 100)
    private String actorName;

    @Column(name = "actor_role", nullable = false, length = 30)
    private String actorRole;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ActionHistory() {}

    public ActionHistory(Long id, Long complaintId, Long actorId, String actorName, String actorRole, String action, String fromStatus, String toStatus, String remarks, boolean isInternal, LocalDateTime createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.remarks = remarks;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long complaintId;
        private Long actorId;
        private String actorName;
        private String actorRole;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String remarks;
        private boolean isInternal = false;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder complaintId(Long complaintId) { this.complaintId = complaintId; return this; }
        public Builder actorId(Long actorId) { this.actorId = actorId; return this; }
        public Builder actorName(String actorName) { this.actorName = actorName; return this; }
        public Builder actorRole(String actorRole) { this.actorRole = actorRole; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder fromStatus(String fromStatus) { this.fromStatus = fromStatus; return this; }
        public Builder toStatus(String toStatus) { this.toStatus = toStatus; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder isInternal(boolean isInternal) { this.isInternal = isInternal; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ActionHistory build() {
            return new ActionHistory(id, complaintId, actorId, actorName, actorRole, action, fromStatus, toStatus, remarks, isInternal, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
