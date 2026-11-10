-- ==============================================================================
-- ResolveTrack: Schema Migration V4
-- Support for Health Score, In-App Notifications, and Follow-Up Engine Settings
-- ==============================================================================

-- 1. Add health_score to complaints
ALTER TABLE complaints ADD COLUMN health_score INT NOT NULL DEFAULT 100;

-- 2. In-App Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE SET NULL,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Settings for Reminder Lead Time, Escalation Delays, Demo Mode & Ignored Nudges
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('demo.mode', 'false', 'When true, all SLA, reminder lead time and escalation units are treated as minutes instead of hours/days'),
('reminder.lead_time', '24', 'Lead time (hours or demo minutes) before follow_up_deadline to send pre-deadline reminder'),
('escalation.delay', '48', 'Delay (hours or demo minutes) past follow_up_deadline before Level 1 escalation'),
('escalation.level2.delay', '96', 'Delay (hours or demo minutes) past follow_up_deadline before Level 2 escalation'),
('reminder.ignored.threshold', '2', 'Number of unacknowledged reminders before triggering early escalation')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

