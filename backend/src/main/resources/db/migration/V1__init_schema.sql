-- ==============================================================================
-- ResolveTrack: Grievance Follow-up and Escalation System Baseline Schema
-- Compliance: Karnataka SAKALA Services Act (2011) Framework
-- ==============================================================================

-- 1. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name_en VARCHAR(150) NOT NULL,
    name_kn VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    default_sla_days INT NOT NULL DEFAULT 7,
    escalation_hours INT NOT NULL DEFAULT 48,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_departments_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL, -- ADMIN, SUPERVISOR, OFFICER, DATA_ENTRY, CITIZEN
    department_id BIGINT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Officers Table
CREATE TABLE IF NOT EXISTS officers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT NOT NULL,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    designation VARCHAR(100) NOT NULL,
    jurisdiction_zone VARCHAR(100),
    active_complaints_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_officer_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_officer_department FOREIGN KEY (department_id) REFERENCES departments (id),
    INDEX idx_officers_empcode (employee_code),
    INDEX idx_officers_department (department_id),
    INDEX idx_officers_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Complaints Table
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_id VARCHAR(32) NOT NULL UNIQUE,
    tracking_number VARCHAR(32) NULL,
    citizen_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    assigned_officer_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, TRIAGED, IN_PROGRESS, RESOLVED, REJECTED, ESCALATED
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    location_address VARCHAR(255) NULL,
    follow_up_deadline TIMESTAMP NOT NULL,
    sla_due_date TIMESTAMP NULL,
    escalation_level INT NOT NULL DEFAULT 0,
    ai_suggested_department_id BIGINT NULL,
    ai_suggested_priority VARCHAR(20) NULL,
    ai_triage_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_citizen FOREIGN KEY (citizen_id) REFERENCES users (id),
    CONSTRAINT fk_complaint_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_complaint_officer FOREIGN KEY (assigned_officer_id) REFERENCES officers (id),
    INDEX idx_complaints_tracking_id (tracking_id),
    INDEX idx_complaints_tracking_number (tracking_number),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_assigned_officer_id (assigned_officer_id),
    INDEX idx_complaints_follow_up_deadline (follow_up_deadline),
    INDEX idx_complaints_citizen (citizen_id),
    INDEX idx_complaints_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Action History Table (Immutable Audit Log)
CREATE TABLE IF NOT EXISTS action_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(100) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NULL,
    remarks TEXT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    INDEX idx_history_complaint (complaint_id),
    INDEX idx_history_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Reminders Table
CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    officer_id BIGINT NOT NULL,
    reminder_type VARCHAR(50) NOT NULL, -- PRE_DEADLINE_NUDGE, OVERDUE_WARNING, SUPERVISOR_ALERT
    nudge_count INT NOT NULL DEFAULT 1,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP', -- IN_APP, SMS, EMAIL
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP NULL,
    CONSTRAINT fk_reminder_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    CONSTRAINT fk_reminder_officer FOREIGN KEY (officer_id) REFERENCES officers (id),
    INDEX idx_reminder_complaint (complaint_id),
    INDEX idx_reminder_officer (officer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Escalations Table
CREATE TABLE IF NOT EXISTS escalations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    from_level INT NOT NULL,
    to_level INT NOT NULL,
    trigger_reason VARCHAR(255) NOT NULL,
    notified_supervisor_id BIGINT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_escalation_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    INDEX idx_escalation_complaint (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. System Settings Table
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
