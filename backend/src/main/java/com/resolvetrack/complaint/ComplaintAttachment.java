package com.resolvetrack.complaint;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_attachments", indexes = {
        @Index(name = "idx_attachment_complaint", columnList = "complaint_id")
})
public class ComplaintAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ComplaintAttachment() {}

    public ComplaintAttachment(Long id, Complaint complaint, String originalFilename, String storedFilename, String filePath, String contentType, Long fileSize, LocalDateTime createdAt) {
        this.id = id;
        this.complaint = complaint;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Complaint complaint;
        private String originalFilename;
        private String storedFilename;
        private String filePath;
        private String contentType;
        private Long fileSize;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder complaint(Complaint complaint) { this.complaint = complaint; return this; }
        public Builder originalFilename(String name) { this.originalFilename = name; return this; }
        public Builder storedFilename(String name) { this.storedFilename = name; return this; }
        public Builder filePath(String path) { this.filePath = path; return this; }
        public Builder contentType(String type) { this.contentType = type; return this; }
        public Builder fileSize(Long size) { this.fileSize = size; return this; }
        public Builder createdAt(LocalDateTime time) { this.createdAt = time; return this; }

        public ComplaintAttachment build() {
            return new ComplaintAttachment(id, complaint, originalFilename, storedFilename, filePath, contentType, fileSize, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

