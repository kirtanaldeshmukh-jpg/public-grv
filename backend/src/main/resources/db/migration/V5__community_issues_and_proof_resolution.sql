-- ==============================================================================
-- ResolveTrack: Schema Migration V5
-- Support for Community Issues (Duplicate Clusters) and Proof-Based Resolution
-- ==============================================================================

-- 1. Community Issues Table for Multi-Citizen Spatial & Thematic Clusters
CREATE TABLE IF NOT EXISTS community_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    location_address VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_issues_dept FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE CASCADE,
    INDEX idx_comm_issues_dept (department_id),
    INDEX idx_comm_issues_category (category),
    INDEX idx_comm_issues_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Alter complaints table: link to community issue, track reopen count, resolution proof
ALTER TABLE complaints ADD COLUMN community_issue_id BIGINT NULL;
ALTER TABLE complaints ADD COLUMN reopen_count INT NOT NULL DEFAULT 0;
ALTER TABLE complaints ADD COLUMN resolution_summary TEXT NULL;
ALTER TABLE complaints ADD COLUMN resolution_submitted_at TIMESTAMP NULL;

ALTER TABLE complaints ADD CONSTRAINT fk_complaints_community_issue 
    FOREIGN KEY (community_issue_id) REFERENCES community_issues (id) ON DELETE SET NULL;

CREATE INDEX idx_complaints_comm_issue ON complaints (community_issue_id);

-- 3. Settings for Duplicate Detection and Citizen Verification Timeout
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('duplicate.jaccard.threshold', '0.35', 'Jaccard similarity threshold on normalized tokens for duplicate detection (0.0 to 1.0)'),
('duplicate.radius.meters', '500', 'Maximum distance in meters for duplicate geographic proximity'),
('resolution.citizen.review.timeout.days', '3', 'Days citizen has to confirm or reject resolution before supervisor review is flagged')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

