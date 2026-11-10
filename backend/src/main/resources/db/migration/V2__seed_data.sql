-- ==============================================================================
-- ResolveTrack: Seed Migration for Baseline Departments, Users, and Settings
-- Seed Data Specification: 5 Departments, 1 Admin, 2 Supervisors, 5 Officers, Default Settings
-- ==============================================================================

-- 1. Seed 5 Departments
INSERT INTO departments (id, code, name_en, name_kn, description, default_sla_days, escalation_hours, active) VALUES
(1, 'POL', 'Police Department', 'ಪೊಲೀಸ್ ಇಲಾಖೆ', 'Law & order, civic non-cognizable grievances, traffic safety', 7, 24, TRUE),
(2, 'CYB', 'Cybercrime Department', 'ಸೈಬರ್ ಅಪರಾಧ ಇಲಾಖೆ', 'Online financial fraud, identity theft, cyber harassment', 5, 24, TRUE),
(3, 'PWD', 'Public Works Department', 'ಲೋಕೋಪಯೋಗಿ ಇಲಾಖೆ', 'State highways, bridge construction, major drainage, government buildings', 10, 48, TRUE),
(4, 'MUN', 'Municipal Administration (BBMP)', 'ನಗರ ಆಡಳಿತ (ಬಿಬಿಎಂಪಿ)', 'Urban roads, pothole repair, solid waste management, property tax', 7, 24, TRUE),
(5, 'ELE', 'Electricity Department (BESCOM)', 'ವಿದ್ಯುತ್ ಸರಬರಾಜು ಇಲಾಖೆ (ಬೆಸ್ಕಾಂ)', 'Power outage, distribution transformer faults, billing anomalies', 3, 12, TRUE);

-- 2. Seed Users
-- Password hashes generated with BCrypt 10 rounds:
-- Admin: Admin@123 -> $2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm
-- Supervisor: Supervisor@123 -> $2a$10$echBaDyt.QBTlcsQ10ZbHOmoNIfmWSMk5h/HE0WuGz2nBLkpHt43m
-- Officer: Officer@123 -> $2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86
-- DataEntry: DataEntry@123 -> $2a$10$qew.zg.kx9bflPJBQQFDBu/cYDfH6KR2RCYoGdB14VJpo.iexvl3q
INSERT INTO users (id, username, email, phone_number, password_hash, full_name, role, department_id, enabled) VALUES
-- 1 Admin
(1, 'admin', 'admin@resolvetrack.gov.in', '+91 80 2200 0001', '$2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm', 'System Administrator', 'ADMIN', NULL, TRUE),

-- 2 Supervisors
(2, 'supervisor_police', 'sup.police@resolvetrack.gov.in', '+91 80 2200 0002', '$2a$10$echBaDyt.QBTlcsQ10ZbHOmoNIfmWSMk5h/HE0WuGz2nBLkpHt43m', 'Superintendent Ramesh Babu (Police & Cybercrime)', 'SUPERVISOR', 1, TRUE),
(3, 'supervisor_works', 'sup.works@resolvetrack.gov.in', '+91 80 2200 0003', '$2a$10$echBaDyt.QBTlcsQ10ZbHOmoNIfmWSMk5h/HE0WuGz2nBLkpHt43m', 'Executive Engineer Shivakumar M (PWD & Civic)', 'SUPERVISOR', 3, TRUE),

-- 5 Officers
(4, 'officer_police', 'rajesh.police@resolvetrack.gov.in', '+91 94480 11001', '$2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86', 'Inspector Rajesh Kumar', 'OFFICER', 1, TRUE),
(5, 'officer_cyber', 'ananya.cyber@resolvetrack.gov.in', '+91 94480 11002', '$2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86', 'Sub-Inspector Ananya Rao', 'OFFICER', 2, TRUE),
(6, 'officer_pwd', 'suresh.pwd@resolvetrack.gov.in', '+91 94480 11003', '$2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86', 'Asst. Executive Engineer Suresh Gowda', 'OFFICER', 3, TRUE),
(7, 'officer_municipal', 'manjunath.mun@resolvetrack.gov.in', '+91 94480 11004', '$2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86', 'Health Inspector Manjunath K', 'OFFICER', 4, TRUE),
(8, 'officer_electricity', 'ramesh.ele@resolvetrack.gov.in', '+91 94480 11005', '$2a$10$bQpyEjdIRJhFOJe0G.zIwObCG1VSnP7qWbUPVW4.yV3jEHANe7.86', 'Section Officer Ramesh B', 'OFFICER', 5, TRUE),

-- 1 Data Entry Operator
(9, 'dataentry_operator', 'dataentry@resolvetrack.gov.in', '+91 80 2200 0009', '$2a$10$qew.zg.kx9bflPJBQQFDBu/cYDfH6KR2RCYoGdB14VJpo.iexvl3q', 'Bengaluru One Desk Operator', 'DATA_ENTRY', NULL, TRUE),

-- 1 Citizen (Default for public lodgements / demo tracking)
(10, 'citizen_kavitha', 'kavitha.citizen@example.com', '+91 98450 12345', '$2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm', 'Kavitha Deshmukh', 'CITIZEN', NULL, TRUE);

-- 3. Seed Officers
INSERT INTO officers (id, user_id, department_id, employee_code, designation, jurisdiction_zone, active_complaints_count) VALUES
(1, 4, 1, 'EMP-POL-001', 'Circle Police Inspector', 'Central Division, Bengaluru', 0),
(2, 5, 2, 'EMP-CYB-001', 'Cybercrime Sub-Inspector', 'Cyber Crime Cell, CID Karnataka', 0),
(3, 6, 3, 'EMP-PWD-001', 'Assistant Executive Engineer', 'Bengaluru Infrastructure Sub-Division', 0),
(4, 7, 4, 'EMP-MUN-001', 'BBMP Ward Health & Works Inspector', 'South Zone - Ward 150', 0),
(5, 8, 5, 'EMP-ELE-001', 'BESCOM Junior Station Engineer', 'Indiranagar Sub-Division', 0);

-- 4. Seed Default System Settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('sla.default.days', '7', 'Default Karnataka SAKALA Service standard resolution window in days'),
('sla.warning.hours', '24', 'Threshold hours before deadline to issue automated officer nudge'),
('escalation.tier1.hours', '48', 'Hours overdue before Tier 1 escalation to Department Supervisor'),
('escalation.tier2.hours', '96', 'Hours overdue before Tier 2 escalation to Appellate Authority / Admin'),
('sakala.compliance.enabled', 'true', 'Enforce Karnataka SAKALA Services Act (2011) statutory compliance tracking'),
('ai.triage.auto_suggest', 'true', 'Enable AI assistive department and priority recommendations (subject to officer confirmation)'),
('portal.maintenance.mode', 'false', 'Global portal maintenance window flag');

