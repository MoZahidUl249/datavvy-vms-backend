-- LOCAL / DEMO ONLY (loaded by the "local" profile, never in production).
-- Passwords: Admin@12345, Manager@12345, Viewer@12345

INSERT INTO app_user (email, display_name, password_hash, role, enabled, created_at, updated_at, created_by, updated_by, version) VALUES
    ('admin@datavvy.com',   'Saif Mahmud',  '$2b$10$6SIhhYuTgm6V82PsSl54guqSk7EKxDqwNJpC/j4JACXaaesOEDBrO', 'ADMIN',         TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('manager@datavvy.com', 'Fleet Manager', '$2b$10$lEH11f3Rr7ST2HNcKKSXg.bqQG6TfMHC18eXLpPoVpEFl1HELWhtG', 'FLEET_MANAGER', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('viewer@datavvy.com',  'Read Only',     '$2b$10$LIUguf.FCwKhUIPsPa5BOudWKgFuvIP0u.hmVYYiU1a28lvuS5E2K', 'VIEWER',        TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0);
