-- =============================================================================
-- Tai khoan quan tri vien dau tien (moi truong dev).
-- Mat khau: Admin@123456 (BCrypt cost 10)
-- DOI MAT KHAU NAY TRUOC KHI TRIEN KHAI LEN PRODUCTION.
-- =============================================================================
INSERT INTO users (email, password_hash, full_name, status, created_at, updated_at)
VALUES ('admin@elearning.hunre.edu.vn',
        '$2a$10$7PSjds09ZJ6Hf9nj7nT1O.FM/FSu03EVWfiZp8cixPok09IvShoUC',
        'Administrator',
        'ACTIVE',
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6));

-- Gan ROLE_ADMIN cho tai khoan vua tao
INSERT INTO user_roles (user_id, role_id, assigned_at)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@elearning.hunre.edu.vn'),
    (SELECT id FROM roles  WHERE code  = 'ROLE_ADMIN'),
    CURRENT_TIMESTAMP(6)
);