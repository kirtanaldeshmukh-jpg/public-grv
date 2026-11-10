package com.resolvetrack.department;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_kn", nullable = false, length = 150)
    private String nameKn;

    private String description;

    @Column(name = "default_sla_days", nullable = false)
    private Integer defaultSlaDays = 7;

    @Column(name = "escalation_hours", nullable = false)
    private Integer escalationHours = 48;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Department() {}

    public Department(Long id, String code, String nameEn, String nameKn, String description, Integer defaultSlaDays, Integer escalationHours, boolean active) {
        this(id, code, nameEn, nameKn, description, defaultSlaDays, escalationHours, active, LocalDateTime.now());
    }

    public Department(Long id, String code, String nameEn, String nameKn, String description, Integer defaultSlaDays, Integer escalationHours, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.nameEn = nameEn;
        this.nameKn = nameKn;
        this.description = description;
        this.defaultSlaDays = defaultSlaDays != null ? defaultSlaDays : 7;
        this.escalationHours = escalationHours != null ? escalationHours : 48;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String code;
        private String nameEn;
        private String nameKn;
        private String description;
        private Integer defaultSlaDays = 7;
        private Integer escalationHours = 48;
        private boolean active = true;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder nameEn(String nameEn) { this.nameEn = nameEn; return this; }
        public Builder nameKn(String nameKn) { this.nameKn = nameKn; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder defaultSlaDays(Integer defaultSlaDays) { this.defaultSlaDays = defaultSlaDays; return this; }
        public Builder escalationHours(Integer escalationHours) { this.escalationHours = escalationHours; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Department build() {
            return new Department(id, code, nameEn, nameKn, description, defaultSlaDays, escalationHours, active, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameKn() { return nameKn; }
    public void setNameKn(String nameKn) { this.nameKn = nameKn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDefaultSlaDays() { return defaultSlaDays; }
    public void setDefaultSlaDays(Integer defaultSlaDays) { this.defaultSlaDays = defaultSlaDays; }
    public Integer getEscalationHours() { return escalationHours; }
    public void setEscalationHours(Integer escalationHours) { this.escalationHours = escalationHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
