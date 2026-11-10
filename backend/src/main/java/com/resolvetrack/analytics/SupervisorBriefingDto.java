package com.resolvetrack.analytics;

public class SupervisorBriefingDto {
    private String date;
    private String greetingEn;
    private String greetingKn;
    private String departmentNameEn;
    private String departmentNameKn;
    private long pendingCount;
    private long overdueCount;
    private long escalationCount;
    private long reopenedCount;
    private long approachingDeadlineCount;
    private String narrativeEn;
    private String narrativeKn;
    private TopPriorityComplaintDto topPriorityComplaint;

    public SupervisorBriefingDto() {}

    public SupervisorBriefingDto(
            String date,
            String greetingEn,
            String greetingKn,
            String departmentNameEn,
            String departmentNameKn,
            long pendingCount,
            long overdueCount,
            long escalationCount,
            long reopenedCount,
            long approachingDeadlineCount,
            String narrativeEn,
            String narrativeKn,
            TopPriorityComplaintDto topPriorityComplaint) {
        this.date = date;
        this.greetingEn = greetingEn;
        this.greetingKn = greetingKn;
        this.departmentNameEn = departmentNameEn;
        this.departmentNameKn = departmentNameKn;
        this.pendingCount = pendingCount;
        this.overdueCount = overdueCount;
        this.escalationCount = escalationCount;
        this.reopenedCount = reopenedCount;
        this.approachingDeadlineCount = approachingDeadlineCount;
        this.narrativeEn = narrativeEn;
        this.narrativeKn = narrativeKn;
        this.topPriorityComplaint = topPriorityComplaint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String date;
        private String greetingEn;
        private String greetingKn;
        private String departmentNameEn;
        private String departmentNameKn;
        private long pendingCount;
        private long overdueCount;
        private long escalationCount;
        private long reopenedCount;
        private long approachingDeadlineCount;
        private String narrativeEn;
        private String narrativeKn;
        private TopPriorityComplaintDto topPriorityComplaint;

        public Builder date(String val) { this.date = val; return this; }
        public Builder greetingEn(String val) { this.greetingEn = val; return this; }
        public Builder greetingKn(String val) { this.greetingKn = val; return this; }
        public Builder departmentNameEn(String val) { this.departmentNameEn = val; return this; }
        public Builder departmentNameKn(String val) { this.departmentNameKn = val; return this; }
        public Builder pendingCount(long val) { this.pendingCount = val; return this; }
        public Builder overdueCount(long val) { this.overdueCount = val; return this; }
        public Builder escalationCount(long val) { this.escalationCount = val; return this; }
        public Builder reopenedCount(long val) { this.reopenedCount = val; return this; }
        public Builder approachingDeadlineCount(long val) { this.approachingDeadlineCount = val; return this; }
        public Builder narrativeEn(String val) { this.narrativeEn = val; return this; }
        public Builder narrativeKn(String val) { this.narrativeKn = val; return this; }
        public Builder topPriorityComplaint(TopPriorityComplaintDto val) { this.topPriorityComplaint = val; return this; }

        public SupervisorBriefingDto build() {
            return new SupervisorBriefingDto(
                    date, greetingEn, greetingKn, departmentNameEn, departmentNameKn,
                    pendingCount, overdueCount, escalationCount, reopenedCount,
                    approachingDeadlineCount, narrativeEn, narrativeKn, topPriorityComplaint
            );
        }
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getGreetingEn() { return greetingEn; }
    public void setGreetingEn(String greetingEn) { this.greetingEn = greetingEn; }
    public String getGreetingKn() { return greetingKn; }
    public void setGreetingKn(String greetingKn) { this.greetingKn = greetingKn; }
    public String getDepartmentNameEn() { return departmentNameEn; }
    public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
    public String getDepartmentNameKn() { return departmentNameKn; }
    public void setDepartmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public long getEscalationCount() { return escalationCount; }
    public void setEscalationCount(long escalationCount) { this.escalationCount = escalationCount; }
    public long getReopenedCount() { return reopenedCount; }
    public void setReopenedCount(long reopenedCount) { this.reopenedCount = reopenedCount; }
    public long getApproachingDeadlineCount() { return approachingDeadlineCount; }
    public void setApproachingDeadlineCount(long approachingDeadlineCount) { this.approachingDeadlineCount = approachingDeadlineCount; }
    public String getNarrativeEn() { return narrativeEn; }
    public void setNarrativeEn(String narrativeEn) { this.narrativeEn = narrativeEn; }
    public String getNarrativeKn() { return narrativeKn; }
    public void setNarrativeKn(String narrativeKn) { this.narrativeKn = narrativeKn; }
    public TopPriorityComplaintDto getTopPriorityComplaint() { return topPriorityComplaint; }
    public void setTopPriorityComplaint(TopPriorityComplaintDto topPriorityComplaint) { this.topPriorityComplaint = topPriorityComplaint; }

    public static class TopPriorityComplaintDto {
        private Long complaintId;
        private String trackingId;
        private String title;
        private String category;
        private String departmentName;
        private String priority;
        private String status;
        private int healthScore;
        private String healthBand;
        private String locationAddress;
        private String deadline;
        private String recommendedAction;
        private String actionKey;

        public TopPriorityComplaintDto() {}

        public TopPriorityComplaintDto(Long complaintId, String trackingId, String title, String category, String departmentName, String priority, String status, int healthScore, String healthBand, String locationAddress, String deadline, String recommendedAction, String actionKey) {
            this.complaintId = complaintId;
            this.trackingId = trackingId;
            this.title = title;
            this.category = category;
            this.departmentName = departmentName;
            this.priority = priority;
            this.status = status;
            this.healthScore = healthScore;
            this.healthBand = healthBand;
            this.locationAddress = locationAddress;
            this.deadline = deadline;
            this.recommendedAction = recommendedAction;
            this.actionKey = actionKey;
        }

        public Long getComplaintId() { return complaintId; }
        public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
        public String getTrackingId() { return trackingId; }
        public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getHealthScore() { return healthScore; }
        public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
        public String getHealthBand() { return healthBand; }
        public void setHealthBand(String healthBand) { this.healthBand = healthBand; }
        public String getLocationAddress() { return locationAddress; }
        public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
        public String getDeadline() { return deadline; }
        public void setDeadline(String deadline) { this.deadline = deadline; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public String getActionKey() { return actionKey; }
        public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    }
}

