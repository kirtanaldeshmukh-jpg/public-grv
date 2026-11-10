package com.resolvetrack.complaint;

import com.resolvetrack.auth.User;
import com.resolvetrack.common.BaseEntity;
import com.resolvetrack.department.Department;
import com.resolvetrack.officer.Officer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_tracking_id", columnList = "tracking_id"),
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_assigned_officer_id", columnList = "assigned_officer_id"),
        @Index(name = "idx_complaints_follow_up_deadline", columnList = "follow_up_deadline")
})
public class Complaint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id", nullable = false, unique = true, length = 32)
    private String trackingId;

    @Column(name = "tracking_number", length = 32)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private Officer assignedOfficer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status = ComplaintStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintSource source = ComplaintSource.WEB;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "location_address")
    private String locationAddress;

    @Column(name = "follow_up_deadline", nullable = false)
    private LocalDateTime followUpDeadline;

    @Column(name = "sla_due_date")
    private LocalDateTime slaDueDate;

    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel = 0;

    @Column(name = "ai_suggested_department_id")
    private Long aiSuggestedDepartmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_suggested_priority", length = 20)
    private Priority aiSuggestedPriority;

    @Column(name = "ai_triage_confirmed", nullable = false)
    private boolean aiTriageConfirmed = false;

    @Column(name = "health_score", nullable = false)
    private Integer healthScore = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_issue_id")
    private CommunityIssue communityIssue;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount = 0;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "resolution_submitted_at")
    private LocalDateTime resolutionSubmittedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void syncDualFields() {
        if (this.trackingId == null && this.trackingNumber != null) {
            this.trackingId = this.trackingNumber;
        } else if (this.trackingNumber == null && this.trackingId != null) {
            this.trackingNumber = this.trackingId;
        }

        if (this.followUpDeadline == null && this.slaDueDate != null) {
            this.followUpDeadline = this.slaDueDate;
        } else if (this.slaDueDate == null && this.followUpDeadline != null) {
            this.slaDueDate = this.followUpDeadline;
        }

        if (this.status == null) {
            this.status = ComplaintStatus.NEW;
        }

        if (this.source == null) {
            this.source = ComplaintSource.WEB;
        }

        if (this.healthScore == null) {
            this.healthScore = 100;
        }

        if (this.reopenCount == null) {
            this.reopenCount = 0;
        }
    }

    public Complaint() {}

    public Complaint(Long id, String trackingId, String trackingNumber, User citizen, Department department, Officer assignedOfficer, String title, String description, String category, Priority priority, ComplaintStatus status, ComplaintSource source, BigDecimal latitude, BigDecimal longitude, String locationAddress, LocalDateTime followUpDeadline, LocalDateTime slaDueDate, Integer escalationLevel, Long aiSuggestedDepartmentId, Priority aiSuggestedPriority, boolean aiTriageConfirmed, List<ComplaintAttachment> attachments) {
        this.id = id;
        this.trackingId = trackingId != null ? trackingId : trackingNumber;
        this.trackingNumber = trackingNumber != null ? trackingNumber : trackingId;
        this.citizen = citizen;
        this.department = department;
        this.assignedOfficer = assignedOfficer;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.status = status != null ? status : ComplaintStatus.NEW;
        this.source = source != null ? source : ComplaintSource.WEB;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationAddress = locationAddress;
        this.followUpDeadline = followUpDeadline != null ? followUpDeadline : slaDueDate;
        this.slaDueDate = slaDueDate != null ? slaDueDate : followUpDeadline;
        this.escalationLevel = escalationLevel != null ? escalationLevel : 0;
        this.aiSuggestedDepartmentId = aiSuggestedDepartmentId;
        this.aiSuggestedPriority = aiSuggestedPriority;
        this.aiTriageConfirmed = aiTriageConfirmed;
        if (attachments != null) {
            this.attachments = attachments;
        }
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String trackingId;
        private String trackingNumber;
        private User citizen;
        private Department department;
        private Officer assignedOfficer;
        private String title;
        private String description;
        private String category;
        private Priority priority = Priority.MEDIUM;
        private ComplaintStatus status = ComplaintStatus.NEW;
        private ComplaintSource source = ComplaintSource.WEB;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String locationAddress;
        private LocalDateTime followUpDeadline;
        private LocalDateTime slaDueDate;
        private Integer escalationLevel = 0;
        private Long aiSuggestedDepartmentId;
        private Priority aiSuggestedPriority;
        private boolean aiTriageConfirmed = false;
        private Integer healthScore = 100;
        private CommunityIssue communityIssue;
        private Integer reopenCount = 0;
        private String resolutionSummary;
        private LocalDateTime resolutionSubmittedAt;
        private List<ComplaintAttachment> attachments = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder trackingId(String trackingId) {
            this.trackingId = trackingId;
            if (this.trackingNumber == null) this.trackingNumber = trackingId;
            return this;
        }
        public Builder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            if (this.trackingId == null) this.trackingId = trackingNumber;
            return this;
        }
        public Builder citizen(User citizen) { this.citizen = citizen; return this; }
        public Builder department(Department department) { this.department = department; return this; }
        public Builder assignedOfficer(Officer assignedOfficer) { this.assignedOfficer = assignedOfficer; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder status(ComplaintStatus status) { this.status = status; return this; }
        public Builder source(ComplaintSource source) { this.source = source; return this; }
        public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
        public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
        public Builder locationAddress(String locationAddress) { this.locationAddress = locationAddress; return this; }
        public Builder followUpDeadline(LocalDateTime followUpDeadline) {
            this.followUpDeadline = followUpDeadline;
            if (this.slaDueDate == null) this.slaDueDate = followUpDeadline;
            return this;
        }
        public Builder slaDueDate(LocalDateTime slaDueDate) {
            this.slaDueDate = slaDueDate;
            if (this.followUpDeadline == null) this.followUpDeadline = slaDueDate;
            return this;
        }
        public Builder escalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; return this; }
        public Builder aiSuggestedDepartmentId(Long id) { this.aiSuggestedDepartmentId = id; return this; }
        public Builder aiSuggestedPriority(Priority p) { this.aiSuggestedPriority = p; return this; }
        public Builder aiTriageConfirmed(boolean confirmed) { this.aiTriageConfirmed = confirmed; return this; }
        public Builder healthScore(Integer healthScore) { this.healthScore = healthScore != null ? healthScore : 100; return this; }
        public Builder communityIssue(CommunityIssue communityIssue) { this.communityIssue = communityIssue; return this; }
        public Builder reopenCount(Integer reopenCount) { this.reopenCount = reopenCount != null ? reopenCount : 0; return this; }
        public Builder resolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; return this; }
        public Builder resolutionSubmittedAt(LocalDateTime dt) { this.resolutionSubmittedAt = dt; return this; }
        public Builder attachments(List<ComplaintAttachment> attachments) { this.attachments = attachments; return this; }

        public Complaint build() {
            Complaint c = new Complaint(id, trackingId, trackingNumber, citizen, department, assignedOfficer, title, description, category, priority, status, source, latitude, longitude, locationAddress, followUpDeadline, slaDueDate, escalationLevel, aiSuggestedDepartmentId, aiSuggestedPriority, aiTriageConfirmed, attachments);
            if (this.healthScore != null) c.setHealthScore(this.healthScore);
            c.setCommunityIssue(this.communityIssue);
            if (this.reopenCount != null) c.setReopenCount(this.reopenCount);
            c.setResolutionSummary(this.resolutionSummary);
            c.setResolutionSubmittedAt(this.resolutionSubmittedAt);
            return c;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingId() {
        return trackingId != null ? trackingId : trackingNumber;
    }
    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
        if (this.trackingNumber == null) this.trackingNumber = trackingId;
    }

    public String getTrackingNumber() {
        return trackingNumber != null ? trackingNumber : trackingId;
    }
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        if (this.trackingId == null) this.trackingId = trackingNumber;
    }

    public User getCitizen() { return citizen; }
    public void setCitizen(User citizen) { this.citizen = citizen; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Officer getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(Officer assignedOfficer) { this.assignedOfficer = assignedOfficer; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public ComplaintSource getSource() { return source; }
    public void setSource(ComplaintSource source) { this.source = source; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public LocalDateTime getFollowUpDeadline() {
        return followUpDeadline != null ? followUpDeadline : slaDueDate;
    }
    public void setFollowUpDeadline(LocalDateTime followUpDeadline) {
        this.followUpDeadline = followUpDeadline;
        if (this.slaDueDate == null) this.slaDueDate = followUpDeadline;
    }

    public LocalDateTime getSlaDueDate() {
        return slaDueDate != null ? slaDueDate : followUpDeadline;
    }
    public void setSlaDueDate(LocalDateTime slaDueDate) {
        this.slaDueDate = slaDueDate;
        if (this.followUpDeadline == null) this.followUpDeadline = slaDueDate;
    }

    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    public Long getAiSuggestedDepartmentId() { return aiSuggestedDepartmentId; }
    public void setAiSuggestedDepartmentId(Long aiSuggestedDepartmentId) { this.aiSuggestedDepartmentId = aiSuggestedDepartmentId; }
    public Priority getAiSuggestedPriority() { return aiSuggestedPriority; }
    public void setAiSuggestedPriority(Priority aiSuggestedPriority) { this.aiSuggestedPriority = aiSuggestedPriority; }
    public boolean isAiTriageConfirmed() { return aiTriageConfirmed; }
    public void setAiTriageConfirmed(boolean aiTriageConfirmed) { this.aiTriageConfirmed = aiTriageConfirmed; }

    public Integer getHealthScore() { return healthScore != null ? healthScore : 100; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore != null ? healthScore : 100; }

    public CommunityIssue getCommunityIssue() { return communityIssue; }
    public void setCommunityIssue(CommunityIssue communityIssue) { this.communityIssue = communityIssue; }

    public Integer getReopenCount() { return reopenCount != null ? reopenCount : 0; }
    public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount != null ? reopenCount : 0; }

    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }

    public LocalDateTime getResolutionSubmittedAt() { return resolutionSubmittedAt; }
    public void setResolutionSubmittedAt(LocalDateTime resolutionSubmittedAt) { this.resolutionSubmittedAt = resolutionSubmittedAt; }

    public List<ComplaintAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ComplaintAttachment> attachments) { this.attachments = attachments; }
    public void addAttachment(ComplaintAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setComplaint(this);
    }
}
