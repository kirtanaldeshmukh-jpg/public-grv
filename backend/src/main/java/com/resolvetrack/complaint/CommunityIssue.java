package com.resolvetrack.complaint;

import com.resolvetrack.department.Department;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_issues", indexes = {
        @Index(name = "idx_comm_issues_dept", columnList = "department_id"),
        @Index(name = "idx_comm_issues_category", columnList = "category"),
        @Index(name = "idx_comm_issues_status", columnList = "status")
})
public class CommunityIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "location_address", length = 255)
    private String locationAddress;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @OneToMany(mappedBy = "communityIssue", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Complaint> complaints = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CommunityIssue() {}

    public CommunityIssue(Long id, String title, String category, Department department, BigDecimal latitude, BigDecimal longitude, String locationAddress, String status) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.department = department;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationAddress = locationAddress;
        this.status = status != null ? status : "OPEN";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String category;
        private Department department;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String locationAddress;
        private String status = "OPEN";

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder department(Department department) { this.department = department; return this; }
        public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
        public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
        public Builder locationAddress(String locationAddress) { this.locationAddress = locationAddress; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public CommunityIssue build() {
            return new CommunityIssue(id, title, category, department, latitude, longitude, locationAddress, status);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Complaint> getComplaints() { return complaints; }
    public void setComplaints(List<Complaint> complaints) { this.complaints = complaints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void addComplaint(Complaint complaint) {
        this.complaints.add(complaint);
        complaint.setCommunityIssue(this);
    }
}

