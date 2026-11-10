package com.resolvetrack.escalation;

import com.resolvetrack.complaint.Complaint;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "escalations")
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "from_level", nullable = false)
    private Integer fromLevel;

    @Column(name = "to_level", nullable = false)
    private Integer toLevel;

    @Column(name = "trigger_reason", nullable = false)
    private String triggerReason;

    @Column(name = "notified_supervisor_id")
    private Long notifiedSupervisorId;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @CreationTimestamp
    @Column(name = "escalated_at", nullable = false, updatable = false)
    private LocalDateTime escalatedAt;

    public Escalation() {}

    public Escalation(Long id, Complaint complaint, Integer fromLevel, Integer toLevel, String triggerReason, Long notifiedSupervisorId, boolean acknowledged, LocalDateTime escalatedAt) {
        this.id = id;
        this.complaint = complaint;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.triggerReason = triggerReason;
        this.notifiedSupervisorId = notifiedSupervisorId;
        this.acknowledged = acknowledged;
        this.escalatedAt = escalatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Complaint complaint;
        private Integer fromLevel;
        private Integer toLevel;
        private String triggerReason;
        private Long notifiedSupervisorId;
        private boolean acknowledged = false;
        private LocalDateTime escalatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder complaint(Complaint complaint) { this.complaint = complaint; return this; }
        public Builder fromLevel(Integer fromLevel) { this.fromLevel = fromLevel; return this; }
        public Builder toLevel(Integer toLevel) { this.toLevel = toLevel; return this; }
        public Builder triggerReason(String triggerReason) { this.triggerReason = triggerReason; return this; }
        public Builder notifiedSupervisorId(Long id) { this.notifiedSupervisorId = id; return this; }
        public Builder acknowledged(boolean acknowledged) { this.acknowledged = acknowledged; return this; }
        public Builder escalatedAt(LocalDateTime time) { this.escalatedAt = time; return this; }

        public Escalation build() {
            return new Escalation(id, complaint, fromLevel, toLevel, triggerReason, notifiedSupervisorId, acknowledged, escalatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public Integer getFromLevel() { return fromLevel; }
    public void setFromLevel(Integer fromLevel) { this.fromLevel = fromLevel; }
    public Integer getToLevel() { return toLevel; }
    public void setToLevel(Integer toLevel) { this.toLevel = toLevel; }
    public String getTriggerReason() { return triggerReason; }
    public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }
    public Long getNotifiedSupervisorId() { return notifiedSupervisorId; }
    public void setNotifiedSupervisorId(Long notifiedSupervisorId) { this.notifiedSupervisorId = notifiedSupervisorId; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
}
