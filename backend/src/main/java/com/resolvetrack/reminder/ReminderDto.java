package com.resolvetrack.reminder;

import java.time.LocalDateTime;

public class ReminderDto {

    public static class Response {
        private Long id;
        private Long complaintId;
        private String complaintTrackingNumber;
        private Long officerId;
        private String reminderType;
        private Integer nudgeCount;
        private String channel;
        private String message;
        private LocalDateTime sentAt;
        private boolean acknowledged;
        private LocalDateTime acknowledgedAt;

        public Response() {}

        public Response(Long id, Long complaintId, String complaintTrackingNumber, Long officerId, String reminderType, Integer nudgeCount, String channel, String message, LocalDateTime sentAt, boolean acknowledged, LocalDateTime acknowledgedAt) {
            this.id = id;
            this.complaintId = complaintId;
            this.complaintTrackingNumber = complaintTrackingNumber;
            this.officerId = officerId;
            this.reminderType = reminderType;
            this.nudgeCount = nudgeCount;
            this.channel = channel;
            this.message = message;
            this.sentAt = sentAt;
            this.acknowledged = acknowledged;
            this.acknowledgedAt = acknowledgedAt;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private Long complaintId;
            private String complaintTrackingNumber;
            private Long officerId;
            private String reminderType;
            private Integer nudgeCount;
            private String channel;
            private String message;
            private LocalDateTime sentAt;
            private boolean acknowledged;
            private LocalDateTime acknowledgedAt;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder complaintId(Long id) { this.complaintId = id; return this; }
            public Builder complaintTrackingNumber(String num) { this.complaintTrackingNumber = num; return this; }
            public Builder officerId(Long id) { this.officerId = id; return this; }
            public Builder reminderType(String type) { this.reminderType = type; return this; }
            public Builder nudgeCount(Integer count) { this.nudgeCount = count; return this; }
            public Builder channel(String channel) { this.channel = channel; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder sentAt(LocalDateTime time) { this.sentAt = time; return this; }
            public Builder acknowledged(boolean ack) { this.acknowledged = ack; return this; }
            public Builder acknowledgedAt(LocalDateTime time) { this.acknowledgedAt = time; return this; }

            public Response build() {
                return new Response(id, complaintId, complaintTrackingNumber, officerId, reminderType, nudgeCount, channel, message, sentAt, acknowledged, acknowledgedAt);
            }
        }

        public Long getId() { return id; }
        public Long getComplaintId() { return complaintId; }
        public String getComplaintTrackingNumber() { return complaintTrackingNumber; }
        public Long getOfficerId() { return officerId; }
        public String getReminderType() { return reminderType; }
        public Integer getNudgeCount() { return nudgeCount; }
        public String getChannel() { return channel; }
        public String getMessage() { return message; }
        public LocalDateTime getSentAt() { return sentAt; }
        public boolean isAcknowledged() { return acknowledged; }
        public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    }
}
