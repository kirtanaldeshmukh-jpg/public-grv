-- ==============================================================================
-- ResolveTrack: Schema Migration V3
-- Support for Complaint Sources (WEB, PHONE, PAPER), Attachments, and Yearly Tracking Sequences
-- ==============================================================================

-- 1. Add source column to complaints table
ALTER TABLE complaints ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'WEB';

-- 2. Tracking Sequences Table (Thread-safe, yearly resetting RSK-{year}-{seq})
CREATE TABLE IF NOT EXISTS tracking_sequences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    current_sequence BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tracking_year (sequence_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initialize 2026 sequence
INSERT INTO tracking_sequences (sequence_year, current_sequence) VALUES (2026, 0)
ON DUPLICATE KEY UPDATE current_sequence = current_sequence;

-- 3. Complaint Attachments Table (Images & PDFs up to 5MB)
CREATE TABLE IF NOT EXISTS complaint_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    INDEX idx_attachment_complaint (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Category-Specific SLA Window Settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('sla.category.fraud', '3', 'SLA resolution days for Cybercrime fraud and online scam grievances'),
('sla.category.lost', '5', 'SLA resolution days for Police lost/stolen property grievances'),
('sla.category.road', '10', 'SLA resolution days for Public Works road and pothole repair grievances'),
('sla.category.water', '7', 'SLA resolution days for Municipal water supply and drainage grievances'),
('sla.category.power', '3', 'SLA resolution days for Electricity power outages and transformer failures'),
('sla.category.default', '7', 'Fallback default SLA resolution days for unmapped categories')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
