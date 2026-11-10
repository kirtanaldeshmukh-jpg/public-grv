package com.resolvetrack.escalation;

import java.time.LocalDateTime;

public class EscalationDto {

    public static class Response {
        private Long id;
        private Long complaintId;
        private String complaintTrackingNumber;
        private String complaintTitle;
        private String departmentNameEn;
        private Integer fromLevel;
        private Integer toLevel;
        private String triggerReason;
        private Long notifiedSupervisorId;
        private boolean acknowledged;
        private LocalDateTime escalatedAt;

        public Response() {}

        public Response(Long id, Long complaintId, String complaintTrackingNumber, String complaintTitle, String departmentNameEn, Integer fromLevel, Integer toLevel, String triggerReason, Long notifiedSupervisorId, boolean acknowledged, LocalDateTime escalatedAt) {
            this.id = id;
            this.complaintId = complaintId;
            this.complaintTrackingNumber = complaintTrackingNumber;
            this.complaintTitle = complaintTitle;
            this.departmentNameEn = departmentNameEn;
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
            private Long complaintId;
            private String complaintTrackingNumber;
            private String complaintTitle;
            private String departmentNameEn;
            private Integer fromLevel;
            private Integer toLevel;
            private String triggerReason;
            private Long notifiedSupervisorId;
            private boolean acknowledged;
            private LocalDateTime escalatedAt;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder complaintId(Long id) { this.complaintId = id; return this; }
            public Builder complaintTrackingNumber(String num) { this.complaintTrackingNumber = num; return this; }
            public Builder complaintTitle(String title) { this.complaintTitle = title; return this; }
            public Builder departmentNameEn(String name) { this.departmentNameEn = name; return this; }
            public Builder fromLevel(Integer lvl) { this.fromLevel = lvl; return this; }
            public Builder toLevel(Integer lvl) { this.toLevel = lvl; return this; }
            public Builder triggerReason(String r) { this.triggerReason = r; return this; }
            public Builder notifiedSupervisorId(Long id) { this.notifiedSupervisorId = id; return this; }
            public Builder acknowledged(boolean ack) { this.acknowledged = ack; return this; }
            public Builder escalatedAt(LocalDateTime time) { this.escalatedAt = time; return this; }

            public Response build() {
                return new Response(id, complaintId, complaintTrackingNumber, complaintTitle, departmentNameEn, fromLevel, toLevel, triggerReason, notifiedSupervisorId, acknowledged, escalatedAt);
            }
        }

        public Long getId() { return id; }
        public Long getComplaintId() { return complaintId; }
        public String getComplaintTrackingNumber() { return complaintTrackingNumber; }
        public String getComplaintTitle() { return complaintTitle; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public Integer getFromLevel() { return fromLevel; }
        public Integer getToLevel() { return toLevel; }
        public String getTriggerReason() { return triggerReason; }
        public Long getNotifiedSupervisorId() { return notifiedSupervisorId; }
        public boolean isAcknowledged() { return acknowledged; }
        public LocalDateTime getEscalatedAt() { return escalatedAt; }
    }
}
