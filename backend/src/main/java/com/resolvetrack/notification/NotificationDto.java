package com.resolvetrack.notification;

import java.time.LocalDateTime;

public class NotificationDto {

    public static class Response {
        private Long id;
        private Long userId;
        private Long complaintId;
        private String complaintTrackingId;
        private String title;
        private String message;
        private String channel;
        private boolean read;
        private LocalDateTime readAt;
        private LocalDateTime createdAt;

        public Response() {}

        public Response(Long id, Long userId, Long complaintId, String complaintTrackingId, String title, String message, String channel, boolean read, LocalDateTime readAt, LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.complaintId = complaintId;
            this.complaintTrackingId = complaintTrackingId;
            this.title = title;
            this.message = message;
            this.channel = channel;
            this.read = read;
            this.readAt = readAt;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getComplaintId() { return complaintId; }
        public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
        public String getComplaintTrackingId() { return complaintTrackingId; }
        public void setComplaintTrackingId(String complaintTrackingId) { this.complaintTrackingId = complaintTrackingId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
        public LocalDateTime getReadAt() { return readAt; }
        public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class UnreadCountResponse {
        private long unreadCount;

        public UnreadCountResponse() {}
        public UnreadCountResponse(long unreadCount) { this.unreadCount = unreadCount; }

        public long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
    }

    public static class Message {
        private String recipient;
        private String channel;
        private String subject;
        private String content;
        private LocalDateTime sentAt;

        public Message() {}

        public Message(String recipient, String channel, String subject, String content, LocalDateTime sentAt) {
            this.recipient = recipient;
            this.channel = channel;
            this.subject = subject;
            this.content = content;
            this.sentAt = sentAt;
        }

        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }
}
