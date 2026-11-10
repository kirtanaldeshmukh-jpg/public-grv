package com.resolvetrack.history;

import java.time.LocalDateTime;

public class ActionHistoryDto {

    public static class InternalView {
        private Long id;
        private Long complaintId;
        private Long actorId;
        private String actorName;
        private String actorRole;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String remarks;
        private boolean isInternal;
        private LocalDateTime createdAt;

        public InternalView() {}

        public InternalView(Long id, Long complaintId, Long actorId, String actorName, String actorRole, String action, String fromStatus, String toStatus, String remarks, boolean isInternal, LocalDateTime createdAt) {
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
            private boolean isInternal;
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

            public InternalView build() {
                return new InternalView(id, complaintId, actorId, actorName, actorRole, action, fromStatus, toStatus, remarks, isInternal, createdAt);
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

    public static class CitizenView {
        private Long id;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String publicRemarks;
        private LocalDateTime timestamp;

        public CitizenView() {}

        public CitizenView(Long id, String action, String fromStatus, String toStatus, String publicRemarks, LocalDateTime timestamp) {
            this.id = id;
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.publicRemarks = publicRemarks;
            this.timestamp = timestamp;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private String action;
            private String fromStatus;
            private String toStatus;
            private String publicRemarks;
            private LocalDateTime timestamp;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder action(String action) { this.action = action; return this; }
            public Builder fromStatus(String fromStatus) { this.fromStatus = fromStatus; return this; }
            public Builder toStatus(String toStatus) { this.toStatus = toStatus; return this; }
            public Builder publicRemarks(String remarks) { this.publicRemarks = remarks; return this; }
            public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

            public CitizenView build() {
                return new CitizenView(id, action, fromStatus, toStatus, publicRemarks, timestamp);
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public String getPublicRemarks() { return publicRemarks; }
        public void setPublicRemarks(String publicRemarks) { this.publicRemarks = publicRemarks; }
        public String getRemarks() { return publicRemarks; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
