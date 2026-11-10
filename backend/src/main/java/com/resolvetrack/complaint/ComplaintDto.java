package com.resolvetrack.complaint;

import com.resolvetrack.history.ActionHistoryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ComplaintDto {

    public static class CreateRequest {
        private Long departmentId;

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        @NotBlank(message = "Category is required")
        private String category;

        private Priority priority;
        private ComplaintSource source = ComplaintSource.WEB;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String locationAddress;

        // Optional citizen details for public submissions
        private String citizenName;
        private String citizenPhone;
        private String citizenEmail;

        public CreateRequest() {}

        public CreateRequest(Long departmentId, String title, String description, String category, Priority priority, BigDecimal latitude, BigDecimal longitude, String locationAddress) {
            this.departmentId = departmentId;
            this.title = title;
            this.description = description;
            this.category = category;
            this.priority = priority;
            this.latitude = latitude;
            this.longitude = longitude;
            this.locationAddress = locationAddress;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long departmentId;
            private String title;
            private String description;
            private String category;
            private Priority priority;
            private ComplaintSource source = ComplaintSource.WEB;
            private BigDecimal latitude;
            private BigDecimal longitude;
            private String locationAddress;
            private String citizenName;
            private String citizenPhone;
            private String citizenEmail;

            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder title(String title) { this.title = title; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder priority(Priority priority) { this.priority = priority; return this; }
            public Builder source(ComplaintSource source) { this.source = source; return this; }
            public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
            public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
            public Builder locationAddress(String locationAddress) { this.locationAddress = locationAddress; return this; }
            public Builder citizenName(String name) { this.citizenName = name; return this; }
            public Builder citizenPhone(String phone) { this.citizenPhone = phone; return this; }
            public Builder citizenEmail(String email) { this.citizenEmail = email; return this; }

            public CreateRequest build() {
                CreateRequest r = new CreateRequest(departmentId, title, description, category, priority, latitude, longitude, locationAddress);
                r.setSource(this.source != null ? this.source : ComplaintSource.WEB);
                r.setCitizenName(this.citizenName);
                r.setCitizenPhone(this.citizenPhone);
                r.setCitizenEmail(this.citizenEmail);
                return r;
            }
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public ComplaintSource getSource() { return source; }
        public void setSource(ComplaintSource source) { this.source = source; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public String getLocationAddress() { return locationAddress; }
        public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
        public String getCitizenName() { return citizenName; }
        public void setCitizenName(String citizenName) { this.citizenName = citizenName; }
        public String getCitizenPhone() { return citizenPhone; }
        public void setCitizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; }
        public String getCitizenEmail() { return citizenEmail; }
        public void setCitizenEmail(String citizenEmail) { this.citizenEmail = citizenEmail; }
    }

    public static class PublicTrackResponse {
        private String status;
        private String departmentName;
        private String latestPublicUpdate;
        private String resolvedOrPending; // "PENDING" or "RESOLVED"
        private boolean resolved;
        private String resolutionSummary;
        private LocalDateTime resolutionSubmittedAt;
        private boolean canConfirmResolution;
        private Integer reopenCount;
        private CommunityIssueResponse communityIssue;

        public PublicTrackResponse() {}

        public PublicTrackResponse(String status, String departmentName, String latestPublicUpdate, String resolvedOrPending, boolean resolved) {
            this.status = status;
            this.departmentName = departmentName;
            this.latestPublicUpdate = latestPublicUpdate;
            this.resolvedOrPending = resolvedOrPending;
            this.resolved = resolved;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String status;
            private String departmentName;
            private String latestPublicUpdate;
            private String resolvedOrPending;
            private boolean resolved;
            private String resolutionSummary;
            private LocalDateTime resolutionSubmittedAt;
            private boolean canConfirmResolution;
            private Integer reopenCount;
            private CommunityIssueResponse communityIssue;

            public Builder status(String status) { this.status = status; return this; }
            public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
            public Builder latestPublicUpdate(String update) { this.latestPublicUpdate = update; return this; }
            public Builder resolvedOrPending(String status) { this.resolvedOrPending = status; return this; }
            public Builder resolved(boolean resolved) { this.resolved = resolved; return this; }
            public Builder resolutionSummary(String summary) { this.resolutionSummary = summary; return this; }
            public Builder resolutionSubmittedAt(LocalDateTime dt) { this.resolutionSubmittedAt = dt; return this; }
            public Builder canConfirmResolution(boolean canConfirm) { this.canConfirmResolution = canConfirm; return this; }
            public Builder reopenCount(Integer count) { this.reopenCount = count; return this; }
            public Builder communityIssue(CommunityIssueResponse c) { this.communityIssue = c; return this; }

            public PublicTrackResponse build() {
                PublicTrackResponse r = new PublicTrackResponse(status, departmentName, latestPublicUpdate, resolvedOrPending, resolved);
                r.setResolutionSummary(resolutionSummary);
                r.setResolutionSubmittedAt(resolutionSubmittedAt);
                r.setCanConfirmResolution(canConfirmResolution);
                r.setReopenCount(reopenCount);
                r.setCommunityIssue(communityIssue);
                return r;
            }
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getLatestPublicUpdate() { return latestPublicUpdate; }
        public void setLatestPublicUpdate(String latestPublicUpdate) { this.latestPublicUpdate = latestPublicUpdate; }
        public String getResolvedOrPending() { return resolvedOrPending; }
        public void setResolvedOrPending(String resolvedOrPending) { this.resolvedOrPending = resolvedOrPending; }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
        public String getResolutionSummary() { return resolutionSummary; }
        public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
        public LocalDateTime getResolutionSubmittedAt() { return resolutionSubmittedAt; }
        public void setResolutionSubmittedAt(LocalDateTime resolutionSubmittedAt) { this.resolutionSubmittedAt = resolutionSubmittedAt; }
        public boolean isCanConfirmResolution() { return canConfirmResolution; }
        public void setCanConfirmResolution(boolean canConfirmResolution) { this.canConfirmResolution = canConfirmResolution; }
        public Integer getReopenCount() { return reopenCount != null ? reopenCount : 0; }
        public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount; }
        public CommunityIssueResponse getCommunityIssue() { return communityIssue; }
        public void setCommunityIssue(CommunityIssueResponse communityIssue) { this.communityIssue = communityIssue; }
    }

    public static class CitizenTrackingResponse {
        private String trackingId;
        private String trackingNumber;
        private String title;
        private String description;
        private String category;
        private String departmentNameEn;
        private String departmentNameKn;
        private ComplaintStatus status;
        private LocalDateTime followUpDeadline;
        private LocalDateTime slaDueDate;
        private LocalDateTime createdAt;
        private Integer escalationLevel;
        private List<ActionHistoryDto.CitizenView> timeline;
        private List<AttachmentResponse> attachments;
        private String resolutionSummary;
        private LocalDateTime resolutionSubmittedAt;
        private boolean canConfirmResolution;
        private Integer reopenCount;
        private CommunityIssueResponse communityIssue;
        private HealthScoreDetailResponse healthScoreDetail;
        private NextBestActionResponse nextBestAction;

        public CitizenTrackingResponse() {}

        public CitizenTrackingResponse(String trackingId, String trackingNumber, String title, String description, String category, String departmentNameEn, String departmentNameKn, ComplaintStatus status, LocalDateTime followUpDeadline, LocalDateTime slaDueDate, LocalDateTime createdAt, Integer escalationLevel, List<ActionHistoryDto.CitizenView> timeline, List<AttachmentResponse> attachments) {
            this.trackingId = trackingId != null ? trackingId : trackingNumber;
            this.trackingNumber = trackingNumber != null ? trackingNumber : trackingId;
            this.title = title;
            this.description = description;
            this.category = category;
            this.departmentNameEn = departmentNameEn;
            this.departmentNameKn = departmentNameKn;
            this.status = status;
            this.followUpDeadline = followUpDeadline != null ? followUpDeadline : slaDueDate;
            this.slaDueDate = slaDueDate != null ? slaDueDate : followUpDeadline;
            this.createdAt = createdAt;
            this.escalationLevel = escalationLevel;
            this.timeline = timeline;
            this.attachments = attachments;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String trackingId;
            private String trackingNumber;
            private String title;
            private String description;
            private String category;
            private String departmentNameEn;
            private String departmentNameKn;
            private ComplaintStatus status;
            private LocalDateTime followUpDeadline;
            private LocalDateTime slaDueDate;
            private LocalDateTime createdAt;
            private Integer escalationLevel;
            private List<ActionHistoryDto.CitizenView> timeline;
            private List<AttachmentResponse> attachments;
            private String resolutionSummary;
            private LocalDateTime resolutionSubmittedAt;
            private boolean canConfirmResolution;
            private Integer reopenCount;
            private CommunityIssueResponse communityIssue;
            private HealthScoreDetailResponse healthScoreDetail;
            private NextBestActionResponse nextBestAction;

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
            public Builder title(String title) { this.title = title; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder departmentNameEn(String name) { this.departmentNameEn = name; return this; }
            public Builder departmentNameKn(String name) { this.departmentNameKn = name; return this; }
            public Builder status(ComplaintStatus status) { this.status = status; return this; }
            public Builder followUpDeadline(LocalDateTime deadline) {
                this.followUpDeadline = deadline;
                if (this.slaDueDate == null) this.slaDueDate = deadline;
                return this;
            }
            public Builder slaDueDate(LocalDateTime slaDueDate) {
                this.slaDueDate = slaDueDate;
                if (this.followUpDeadline == null) this.followUpDeadline = slaDueDate;
                return this;
            }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder escalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; return this; }
            public Builder timeline(List<ActionHistoryDto.CitizenView> timeline) { this.timeline = timeline; return this; }
            public Builder attachments(List<AttachmentResponse> attachments) { this.attachments = attachments; return this; }
            public Builder resolutionSummary(String summary) { this.resolutionSummary = summary; return this; }
            public Builder resolutionSubmittedAt(LocalDateTime dt) { this.resolutionSubmittedAt = dt; return this; }
            public Builder canConfirmResolution(boolean canConfirm) { this.canConfirmResolution = canConfirm; return this; }
            public Builder reopenCount(Integer count) { this.reopenCount = count; return this; }
            public Builder communityIssue(CommunityIssueResponse issue) { this.communityIssue = issue; return this; }
            public Builder healthScoreDetail(HealthScoreDetailResponse health) { this.healthScoreDetail = health; return this; }
            public Builder nextBestAction(NextBestActionResponse nextBestAction) { this.nextBestAction = nextBestAction; return this; }

            public CitizenTrackingResponse build() {
                CitizenTrackingResponse r = new CitizenTrackingResponse(trackingId, trackingNumber, title, description, category, departmentNameEn, departmentNameKn, status, followUpDeadline, slaDueDate, createdAt, escalationLevel, timeline, attachments);
                r.setResolutionSummary(resolutionSummary);
                r.setResolutionSubmittedAt(resolutionSubmittedAt);
                r.setCanConfirmResolution(canConfirmResolution);
                r.setReopenCount(reopenCount);
                r.setCommunityIssue(communityIssue);
                r.setHealthScoreDetail(healthScoreDetail);
                r.setNextBestAction(nextBestAction);
                return r;
            }
        }

        public String getTrackingId() { return trackingId != null ? trackingId : trackingNumber; }
        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
            if (this.trackingNumber == null) this.trackingNumber = trackingId;
        }

        public String getTrackingNumber() { return trackingNumber != null ? trackingNumber : trackingId; }
        public void setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            if (this.trackingId == null) this.trackingId = trackingNumber;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
        public String getDepartmentNameKn() { return departmentNameKn; }
        public void setDepartmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; }
        public ComplaintStatus getStatus() { return status; }
        public void setStatus(ComplaintStatus status) { this.status = status; }
        public LocalDateTime getFollowUpDeadline() { return followUpDeadline != null ? followUpDeadline : slaDueDate; }
        public void setFollowUpDeadline(LocalDateTime followUpDeadline) {
            this.followUpDeadline = followUpDeadline;
            if (this.slaDueDate == null) this.slaDueDate = followUpDeadline;
        }
        public LocalDateTime getSlaDueDate() { return slaDueDate != null ? slaDueDate : followUpDeadline; }
        public void setSlaDueDate(LocalDateTime slaDueDate) {
            this.slaDueDate = slaDueDate;
            if (this.followUpDeadline == null) this.followUpDeadline = slaDueDate;
        }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Integer getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
        public List<ActionHistoryDto.CitizenView> getTimeline() { return timeline; }
        public void setTimeline(List<ActionHistoryDto.CitizenView> timeline) { this.timeline = timeline; }
        public List<AttachmentResponse> getAttachments() { return attachments; }
        public void setAttachments(List<AttachmentResponse> attachments) { this.attachments = attachments; }
        public String getResolutionSummary() { return resolutionSummary; }
        public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
        public LocalDateTime getResolutionSubmittedAt() { return resolutionSubmittedAt; }
        public void setResolutionSubmittedAt(LocalDateTime resolutionSubmittedAt) { this.resolutionSubmittedAt = resolutionSubmittedAt; }
        public boolean isCanConfirmResolution() { return canConfirmResolution; }
        public void setCanConfirmResolution(boolean canConfirmResolution) { this.canConfirmResolution = canConfirmResolution; }
        public Integer getReopenCount() { return reopenCount != null ? reopenCount : 0; }
        public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount; }
        public CommunityIssueResponse getCommunityIssue() { return communityIssue; }
        public void setCommunityIssue(CommunityIssueResponse communityIssue) { this.communityIssue = communityIssue; }
        public HealthScoreDetailResponse getHealthScoreDetail() { return healthScoreDetail; }
        public void setHealthScoreDetail(HealthScoreDetailResponse healthScoreDetail) { this.healthScoreDetail = healthScoreDetail; }
        public NextBestActionResponse getNextBestAction() { return nextBestAction; }
        public void setNextBestAction(NextBestActionResponse nextBestAction) { this.nextBestAction = nextBestAction; }
    }

    public static class OfficerDetailResponse {
        private Long id;
        private String trackingId;
        private String trackingNumber;
        private Long citizenId;
        private String citizenName;
        private String citizenPhone;
        private Long departmentId;
        private String departmentNameEn;
        private Long assignedOfficerId;
        private String assignedOfficerName;
        private String title;
        private String description;
        private String category;
        private Priority priority;
        private ComplaintStatus status;
        private ComplaintSource source;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String locationAddress;
        private LocalDateTime followUpDeadline;
        private LocalDateTime slaDueDate;
        private Integer escalationLevel;
        private boolean isOverdue;
        private Long aiSuggestedDepartmentId;
        private Priority aiSuggestedPriority;
        private boolean aiTriageConfirmed;
        private Integer healthScore;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ActionHistoryDto.InternalView> history;
        private List<AttachmentResponse> attachments;
        private HealthScoreDetailResponse healthScoreDetail;
        private NextBestActionResponse nextBestAction;
        private CommunityIssueResponse communityIssue;
        private Integer reopenCount;
        private String resolutionSummary;
        private LocalDateTime resolutionSubmittedAt;
        private boolean canConfirmResolution;

        public OfficerDetailResponse() {}

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private String trackingId;
            private String trackingNumber;
            private Long citizenId;
            private String citizenName;
            private String citizenPhone;
            private Long departmentId;
            private String departmentNameEn;
            private Long assignedOfficerId;
            private String assignedOfficerName;
            private String title;
            private String description;
            private String category;
            private Priority priority;
            private ComplaintStatus status;
            private ComplaintSource source = ComplaintSource.WEB;
            private BigDecimal latitude;
            private BigDecimal longitude;
            private String locationAddress;
            private LocalDateTime followUpDeadline;
            private LocalDateTime slaDueDate;
            private Integer escalationLevel;
            private boolean isOverdue;
            private Long aiSuggestedDepartmentId;
            private Priority aiSuggestedPriority;
            private boolean aiTriageConfirmed;
            private Integer healthScore;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;
            private List<ActionHistoryDto.InternalView> history;
            private List<AttachmentResponse> attachments;
            private HealthScoreDetailResponse healthScoreDetail;
            private NextBestActionResponse nextBestAction;
            private CommunityIssueResponse communityIssue;
            private Integer reopenCount;
            private String resolutionSummary;
            private LocalDateTime resolutionSubmittedAt;
            private boolean canConfirmResolution;

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
            public Builder citizenId(Long citizenId) { this.citizenId = citizenId; return this; }
            public Builder citizenName(String citizenName) { this.citizenName = citizenName; return this; }
            public Builder citizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; return this; }
            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder departmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; return this; }
            public Builder assignedOfficerId(Long id) { this.assignedOfficerId = id; return this; }
            public Builder assignedOfficerName(String name) { this.assignedOfficerName = name; return this; }
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
            public Builder isOverdue(boolean isOverdue) { this.isOverdue = isOverdue; return this; }
            public Builder aiSuggestedDepartmentId(Long id) { this.aiSuggestedDepartmentId = id; return this; }
            public Builder aiSuggestedPriority(Priority p) { this.aiSuggestedPriority = p; return this; }
            public Builder aiTriageConfirmed(boolean confirmed) { this.aiTriageConfirmed = confirmed; return this; }
            public Builder healthScore(Integer healthScore) { this.healthScore = healthScore; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
            public Builder history(List<ActionHistoryDto.InternalView> history) { this.history = history; return this; }
            public Builder attachments(List<AttachmentResponse> attachments) { this.attachments = attachments; return this; }
            public Builder healthScoreDetail(HealthScoreDetailResponse h) { this.healthScoreDetail = h; return this; }
            public Builder nextBestAction(NextBestActionResponse n) { this.nextBestAction = n; return this; }
            public Builder communityIssue(CommunityIssueResponse c) { this.communityIssue = c; return this; }
            public Builder reopenCount(Integer r) { this.reopenCount = r; return this; }
            public Builder resolutionSummary(String s) { this.resolutionSummary = s; return this; }
            public Builder resolutionSubmittedAt(LocalDateTime d) { this.resolutionSubmittedAt = d; return this; }
            public Builder canConfirmResolution(boolean c) { this.canConfirmResolution = c; return this; }

            public OfficerDetailResponse build() {
                OfficerDetailResponse r = new OfficerDetailResponse();
                r.id = this.id;
                r.trackingId = this.trackingId != null ? this.trackingId : this.trackingNumber;
                r.trackingNumber = this.trackingNumber != null ? this.trackingNumber : this.trackingId;
                r.citizenId = this.citizenId;
                r.citizenName = this.citizenName;
                r.citizenPhone = this.citizenPhone;
                r.departmentId = this.departmentId;
                r.departmentNameEn = this.departmentNameEn;
                r.assignedOfficerId = this.assignedOfficerId;
                r.assignedOfficerName = this.assignedOfficerName;
                r.title = this.title;
                r.description = this.description;
                r.category = this.category;
                r.priority = this.priority;
                r.status = this.status;
                r.source = this.source;
                r.latitude = this.latitude;
                r.longitude = this.longitude;
                r.locationAddress = this.locationAddress;
                r.followUpDeadline = this.followUpDeadline != null ? this.followUpDeadline : this.slaDueDate;
                r.slaDueDate = this.slaDueDate != null ? this.slaDueDate : this.followUpDeadline;
                r.escalationLevel = this.escalationLevel;
                r.isOverdue = this.isOverdue;
                r.aiSuggestedDepartmentId = this.aiSuggestedDepartmentId;
                r.aiSuggestedPriority = this.aiSuggestedPriority;
                r.aiTriageConfirmed = this.aiTriageConfirmed;
                r.healthScore = this.healthScore != null ? this.healthScore : 100;
                r.createdAt = this.createdAt;
                r.updatedAt = this.updatedAt;
                r.history = this.history;
                r.attachments = this.attachments;
                r.healthScoreDetail = this.healthScoreDetail;
                r.nextBestAction = this.nextBestAction;
                r.communityIssue = this.communityIssue;
                r.reopenCount = this.reopenCount != null ? this.reopenCount : 0;
                r.resolutionSummary = this.resolutionSummary;
                r.resolutionSubmittedAt = this.resolutionSubmittedAt;
                r.canConfirmResolution = this.canConfirmResolution;
                return r;
            }
        }

        public Long getId() { return id; }
        public String getTrackingId() { return trackingId != null ? trackingId : trackingNumber; }
        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
            if (this.trackingNumber == null) this.trackingNumber = trackingId;
        }
        public String getTrackingNumber() { return trackingNumber != null ? trackingNumber : trackingId; }
        public void setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            if (this.trackingId == null) this.trackingId = trackingNumber;
        }
        public Long getCitizenId() { return citizenId; }
        public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }
        public String getCitizenName() { return citizenName; }
        public void setCitizenName(String citizenName) { this.citizenName = citizenName; }
        public String getCitizenPhone() { return citizenPhone; }
        public void setCitizenPhone(String citizenPhone) { this.citizenPhone = citizenPhone; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
        public Long getAssignedOfficerId() { return assignedOfficerId; }
        public void setAssignedOfficerId(Long assignedOfficerId) { this.assignedOfficerId = assignedOfficerId; }
        public String getAssignedOfficerName() { return assignedOfficerName; }
        public void setAssignedOfficerName(String assignedOfficerName) { this.assignedOfficerName = assignedOfficerName; }
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
        public LocalDateTime getFollowUpDeadline() { return followUpDeadline != null ? followUpDeadline : slaDueDate; }
        public void setFollowUpDeadline(LocalDateTime followUpDeadline) {
            this.followUpDeadline = followUpDeadline;
            if (this.slaDueDate == null) this.slaDueDate = followUpDeadline;
        }
        public LocalDateTime getSlaDueDate() { return slaDueDate != null ? slaDueDate : followUpDeadline; }
        public void setSlaDueDate(LocalDateTime slaDueDate) {
            this.slaDueDate = slaDueDate;
            if (this.followUpDeadline == null) this.followUpDeadline = slaDueDate;
        }
        public Integer getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
        public boolean isOverdue() { return isOverdue; }
        public void setOverdue(boolean overdue) { isOverdue = overdue; }
        public Long getAiSuggestedDepartmentId() { return aiSuggestedDepartmentId; }
        public void setAiSuggestedDepartmentId(Long aiSuggestedDepartmentId) { this.aiSuggestedDepartmentId = aiSuggestedDepartmentId; }
        public Priority getAiSuggestedPriority() { return aiSuggestedPriority; }
        public void setAiSuggestedPriority(Priority aiSuggestedPriority) { this.aiSuggestedPriority = aiSuggestedPriority; }
        public boolean isAiTriageConfirmed() { return aiTriageConfirmed; }
        public void setAiTriageConfirmed(boolean aiTriageConfirmed) { this.aiTriageConfirmed = aiTriageConfirmed; }
        public Integer getHealthScore() { return healthScore; }
        public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public List<ActionHistoryDto.InternalView> getHistory() { return history; }
        public void setHistory(List<ActionHistoryDto.InternalView> history) { this.history = history; }
        public List<AttachmentResponse> getAttachments() { return attachments; }
        public void setAttachments(List<AttachmentResponse> attachments) { this.attachments = attachments; }
        public String getResolutionSummary() { return resolutionSummary; }
        public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
        public LocalDateTime getResolutionSubmittedAt() { return resolutionSubmittedAt; }
        public void setResolutionSubmittedAt(LocalDateTime resolutionSubmittedAt) { this.resolutionSubmittedAt = resolutionSubmittedAt; }
        public boolean isCanConfirmResolution() { return canConfirmResolution; }
        public void setCanConfirmResolution(boolean canConfirmResolution) { this.canConfirmResolution = canConfirmResolution; }
        public Integer getReopenCount() { return reopenCount != null ? reopenCount : 0; }
        public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount; }
        public CommunityIssueResponse getCommunityIssue() { return communityIssue; }
        public void setCommunityIssue(CommunityIssueResponse communityIssue) { this.communityIssue = communityIssue; }
        public HealthScoreDetailResponse getHealthScoreDetail() { return healthScoreDetail; }
        public void setHealthScoreDetail(HealthScoreDetailResponse healthScoreDetail) { this.healthScoreDetail = healthScoreDetail; }
        public NextBestActionResponse getNextBestAction() { return nextBestAction; }
        public void setNextBestAction(NextBestActionResponse nextBestAction) { this.nextBestAction = nextBestAction; }
    }

    public static class AttachmentResponse {
        private Long id;
        private String originalFilename;
        private String contentType;
        private Long fileSize;
        private LocalDateTime createdAt;

        public AttachmentResponse() {}
        public AttachmentResponse(Long id, String originalFilename, String contentType, Long fileSize, LocalDateTime createdAt) {
            this.id = id;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ProgressUpdateRequest {
        private ComplaintStatus status;

        @NotBlank(message = "Progress note is required")
        private String progressNote;

        @com.fasterxml.jackson.annotation.JsonAlias({"isPublic", "public"})
        private Boolean publicTimeline = false;
        private LocalDateTime nextFollowUpDate;

        public ProgressUpdateRequest() {}

        public ProgressUpdateRequest(ComplaintStatus status, String progressNote, Boolean publicTimeline, LocalDateTime nextFollowUpDate) {
            this.status = status;
            this.progressNote = progressNote;
            this.publicTimeline = publicTimeline != null ? publicTimeline : false;
            this.nextFollowUpDate = nextFollowUpDate;
        }

        public ComplaintStatus getStatus() { return status; }
        public void setStatus(ComplaintStatus status) { this.status = status; }
        public String getProgressNote() { return progressNote; }
        public void setProgressNote(String progressNote) { this.progressNote = progressNote; }
        public Boolean getPublicTimeline() { return publicTimeline != null ? publicTimeline : false; }
        public void setPublicTimeline(Boolean publicTimeline) { this.publicTimeline = publicTimeline; }
        public void setIsPublic(Boolean isPublic) { this.publicTimeline = isPublic; }
        public Boolean getIsPublic() { return this.publicTimeline; }
        public LocalDateTime getNextFollowUpDate() { return nextFollowUpDate; }
        public void setNextFollowUpDate(LocalDateTime nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }
    }

    public static class StatusUpdateRequest {
        @NotNull(message = "New status is required")
        private ComplaintStatus status;

        @NotBlank(message = "Action remarks are required for audit trail")
        private String remarks;

        private boolean isInternal;

        public StatusUpdateRequest() {}
        public ComplaintStatus getStatus() { return status; }
        public void setStatus(ComplaintStatus status) { this.status = status; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public boolean isInternal() { return isInternal; }
        public void setInternal(boolean internal) { isInternal = internal; }
    }

    public static class AssignOfficerRequest {
        @NotNull(message = "Officer ID is required")
        private Long officerId;

        @com.fasterxml.jackson.annotation.JsonAlias({"reassignReason", "assignmentReason"})
        private String reason;

        public AssignOfficerRequest() {}
        public AssignOfficerRequest(Long officerId, String reason) {
            this.officerId = officerId;
            this.reason = reason;
        }

        public Long getOfficerId() { return officerId; }
        public void setOfficerId(Long officerId) { this.officerId = officerId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public void setReassignReason(String reassignReason) {
            if (this.reason == null || this.reason.isBlank()) {
                this.reason = reassignReason;
            }
        }
        public String getReassignReason() { return reason; }
    }

    public static class ConfirmAiTriageRequest {
        @NotNull(message = "Confirmed department ID is required")
        private Long departmentId;

        @NotNull(message = "Confirmed priority is required")
        private Priority priority;

        private String officerRemarks;

        public ConfirmAiTriageRequest() {}
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public String getOfficerRemarks() { return officerRemarks; }
        public void setOfficerRemarks(String officerRemarks) { this.officerRemarks = officerRemarks; }
    }

    public static class FilterRequest {
        private Long departmentId;
        private ComplaintStatus status;
        private Priority priority;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String search;
        private int page = 0;
        private int size = 10;
        private String sortBy = "healthScore";
        private String sortDir = "asc";

        public FilterRequest() {}

        public FilterRequest(Long departmentId, ComplaintStatus status, Priority priority, LocalDateTime startDate, LocalDateTime endDate, String search, int page, int size, String sortBy, String sortDir) {
            this.departmentId = departmentId;
            this.status = status;
            this.priority = priority;
            this.startDate = startDate;
            this.endDate = endDate;
            this.search = search;
            this.page = page;
            this.size = size;
            this.sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "healthScore";
            this.sortDir = sortDir != null && !sortDir.isBlank() ? sortDir : "asc";
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long departmentId;
            private ComplaintStatus status;
            private Priority priority;
            private LocalDateTime startDate;
            private LocalDateTime endDate;
            private String search;
            private int page = 0;
            private int size = 10;
            private String sortBy = "healthScore";
            private String sortDir = "asc";

            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder status(ComplaintStatus status) { this.status = status; return this; }
            public Builder priority(Priority priority) { this.priority = priority; return this; }
            public Builder startDate(LocalDateTime startDate) { this.startDate = startDate; return this; }
            public Builder endDate(LocalDateTime endDate) { this.endDate = endDate; return this; }
            public Builder search(String search) { this.search = search; return this; }
            public Builder page(int page) { this.page = page; return this; }
            public Builder size(int size) { this.size = size; return this; }
            public Builder sortBy(String sortBy) { this.sortBy = sortBy; return this; }
            public Builder sortDir(String sortDir) { this.sortDir = sortDir; return this; }

            public FilterRequest build() {
                return new FilterRequest(departmentId, status, priority, startDate, endDate, search, page, size, sortBy, sortDir);
            }
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public ComplaintStatus getStatus() { return status; }
        public void setStatus(ComplaintStatus status) { this.status = status; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public String getSearch() { return search; }
        public void setSearch(String search) { this.search = search; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        public String getSortDir() { return sortDir; }
        public void setSortDir(String sortDir) { this.sortDir = sortDir; }
    }

    public static class HealthScoreDetailResponse {
        private int score;
        private String band;
        private List<String> reasons;
        private List<ReasonItemResponse> reasonItems;

        public HealthScoreDetailResponse() {}
        public HealthScoreDetailResponse(int score, String band, List<String> reasons) {
            this(score, band, reasons, null);
        }
        public HealthScoreDetailResponse(int score, String band, List<String> reasons, List<ReasonItemResponse> reasonItems) {
            this.score = score;
            this.band = band;
            this.reasons = reasons;
            this.reasonItems = reasonItems;
        }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public String getBand() { return band; }
        public void setBand(String band) { this.band = band; }
        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
        public List<ReasonItemResponse> getReasonItems() { return reasonItems; }
        public void setReasonItems(List<ReasonItemResponse> reasonItems) { this.reasonItems = reasonItems; }

        public static class ReasonItemResponse {
            private String key;
            private Map<String, Object> params;
            private String message;

            public ReasonItemResponse() {}
            public ReasonItemResponse(String key, Map<String, Object> params, String message) {
                this.key = key;
                this.params = params;
                this.message = message;
            }

            public String getKey() { return key; }
            public void setKey(String key) { this.key = key; }
            public Map<String, Object> getParams() { return params; }
            public void setParams(Map<String, Object> params) { this.params = params; }
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
        }
    }

    public static class NextBestActionResponse {
        private String action;
        private String title;
        private String reason;
        private String targetRole;
        private String urgency;
        private String actionKey;
        private String reasonKey;
        private Map<String, Object> params;

        public NextBestActionResponse() {}
        public NextBestActionResponse(String action, String title, String reason, String targetRole, String urgency) {
            this(action, title, reason, targetRole, urgency, "nba.action." + action, "nba.reason.default", null);
        }
        public NextBestActionResponse(String action, String title, String reason, String targetRole, String urgency, String actionKey, String reasonKey, Map<String, Object> params) {
            this.action = action;
            this.title = title;
            this.reason = reason;
            this.targetRole = targetRole;
            this.urgency = urgency;
            this.actionKey = actionKey != null ? actionKey : "nba.action." + action;
            this.reasonKey = reasonKey != null ? reasonKey : "nba.reason.default";
            this.params = params != null ? params : Collections.emptyMap();
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
        public String getActionKey() { return actionKey; }
        public void setActionKey(String actionKey) { this.actionKey = actionKey; }
        public String getReasonKey() { return reasonKey; }
        public void setReasonKey(String reasonKey) { this.reasonKey = reasonKey; }
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
    }

    public static class CommunityIssueResponse {
        private Long id;
        private String title;
        private String category;
        private String status;
        private LocalDateTime createdAt;
        private List<LinkedComplaintSummary> linkedComplaints;

        public CommunityIssueResponse() {}
        public CommunityIssueResponse(Long id, String title, String category, String status, LocalDateTime createdAt, List<LinkedComplaintSummary> linkedComplaints) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.status = status;
            this.createdAt = createdAt;
            this.linkedComplaints = linkedComplaints;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<LinkedComplaintSummary> getLinkedComplaints() { return linkedComplaints; }
        public void setLinkedComplaints(List<LinkedComplaintSummary> linkedComplaints) { this.linkedComplaints = linkedComplaints; }
    }

    public static class LinkedComplaintSummary {
        private Long id;
        private String trackingId;
        private String title;
        private ComplaintStatus status;
        private LocalDateTime createdAt;

        public LinkedComplaintSummary() {}
        public LinkedComplaintSummary(Long id, String trackingId, String title, ComplaintStatus status, LocalDateTime createdAt) {
            this.id = id;
            this.trackingId = trackingId;
            this.title = title;
            this.status = status;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTrackingId() { return trackingId; }
        public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public ComplaintStatus getStatus() { return status; }
        public void setStatus(ComplaintStatus status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class SubmitResolutionRequest {
        @NotBlank(message = "Resolution summary is required")
        private String summary;

        public SubmitResolutionRequest() {}
        public SubmitResolutionRequest(String summary) { this.summary = summary; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class ConfirmResolutionRequest {
        private String feedback;

        public ConfirmResolutionRequest() {}
        public ConfirmResolutionRequest(String feedback) { this.feedback = feedback; }

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class RejectResolutionRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;

        public RejectResolutionRequest() {}
        public RejectResolutionRequest(String reason) { this.reason = reason; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
