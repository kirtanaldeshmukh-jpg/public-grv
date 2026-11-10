package com.resolvetrack.reminder;

import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.officer.Officer;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private Officer officer;

    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType;

    @Column(name = "nudge_count", nullable = false)
    private Integer nudgeCount = 1;

    @Column(nullable = false, length = 30)
    private String channel = "IN_APP";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    public Reminder() {}

    public Reminder(Long id, Complaint complaint, Officer officer, String reminderType, Integer nudgeCount, String channel, String message, LocalDateTime sentAt, boolean acknowledged, LocalDateTime acknowledgedAt) {
        this.id = id;
        this.complaint = complaint;
        this.officer = officer;
        this.reminderType = reminderType;
        this.nudgeCount = nudgeCount != null ? nudgeCount : 1;
        this.channel = channel != null ? channel : "IN_APP";
        this.message = message;
        this.sentAt = sentAt;
        this.acknowledged = acknowledged;
        this.acknowledgedAt = acknowledgedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Complaint complaint;
        private Officer officer;
        private String reminderType;
        private Integer nudgeCount = 1;
        private String channel = "IN_APP";
        private String message;
        private LocalDateTime sentAt;
        private boolean acknowledged = false;
        private LocalDateTime acknowledgedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder complaint(Complaint complaint) { this.complaint = complaint; return this; }
        public Builder officer(Officer officer) { this.officer = officer; return this; }
        public Builder reminderType(String reminderType) { this.reminderType = reminderType; return this; }
        public Builder nudgeCount(Integer nudgeCount) { this.nudgeCount = nudgeCount; return this; }
        public Builder channel(String channel) { this.channel = channel; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder sentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
        public Builder acknowledged(boolean acknowledged) { this.acknowledged = acknowledged; return this; }
        public Builder acknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; return this; }

        public Reminder build() {
            return new Reminder(id, complaint, officer, reminderType, nudgeCount, channel, message, sentAt, acknowledged, acknowledgedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public Officer getOfficer() { return officer; }
    public void setOfficer(Officer officer) { this.officer = officer; }
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    public Integer getNudgeCount() { return nudgeCount; }
    public void setNudgeCount(Integer nudgeCount) { this.nudgeCount = nudgeCount; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
