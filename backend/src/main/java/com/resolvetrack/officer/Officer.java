package com.resolvetrack.officer;

import com.resolvetrack.auth.User;
import com.resolvetrack.common.BaseEntity;
import com.resolvetrack.department.Department;
import jakarta.persistence.*;

@Entity
@Table(name = "officers")
public class Officer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(name = "jurisdiction_zone", length = 100)
    private String jurisdictionZone;

    @Column(name = "active_complaints_count", nullable = false)
    private Integer activeComplaintsCount = 0;

    public Officer() {}

    public Officer(Long id, User user, Department department, String employeeCode, String designation, String jurisdictionZone, Integer activeComplaintsCount) {
        this.id = id;
        this.user = user;
        this.department = department;
        this.employeeCode = employeeCode;
        this.designation = designation;
        this.jurisdictionZone = jurisdictionZone;
        this.activeComplaintsCount = activeComplaintsCount != null ? activeComplaintsCount : 0;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User user;
        private Department department;
        private String employeeCode;
        private String designation;
        private String jurisdictionZone;
        private Integer activeComplaintsCount = 0;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder department(Department department) { this.department = department; return this; }
        public Builder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder jurisdictionZone(String jurisdictionZone) { this.jurisdictionZone = jurisdictionZone; return this; }
        public Builder activeComplaintsCount(Integer count) { this.activeComplaintsCount = count; return this; }

        public Officer build() {
            return new Officer(id, user, department, employeeCode, designation, jurisdictionZone, activeComplaintsCount);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getJurisdictionZone() { return jurisdictionZone; }
    public void setJurisdictionZone(String jurisdictionZone) { this.jurisdictionZone = jurisdictionZone; }
    public Integer getActiveComplaintsCount() { return activeComplaintsCount; }
    public void setActiveComplaintsCount(Integer activeComplaintsCount) { this.activeComplaintsCount = activeComplaintsCount; }
}
