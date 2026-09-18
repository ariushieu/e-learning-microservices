-- =============================================================================
-- auth_db - Sở hữu bởi auth-service
-- Quản lý người dùng, phân quyền, phiên đăng nhập (JWT refresh token).
-- Không service nào khác được truy cập trực tiếp database này.
-- =============================================================================

-- Người dùng: học viên, giảng viên, quản trị viên
CREATE TABLE users
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL COMMENT 'BCrypt, 60 ký tự',
    full_name         VARCHAR(150) NOT NULL,
    phone             VARCHAR(20)      NULL,
    avatar_url        VARCHAR(500)     NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    email_verified_at DATETIME(6)      NULL,
    last_login_at     DATETIME(6)      NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status (status),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Tài khoản người dùng';

-- Vai trò (bảng tham chiếu, dữ liệu do V2 nạp sẵn)
CREATE TABLE roles
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30) NOT NULL COMMENT 'ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN',
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255)     NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Danh mục vai trò';

-- Gán vai trò cho người dùng (nhiều - nhiều)
CREATE TABLE user_roles
(
    user_id     BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Người dùng - vai trò';

-- Refresh token: lưu hash thay vì token gốc, để rò rỉ DB không dùng lại được token
CREATE TABLE refresh_tokens
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash CHAR(64)    NOT NULL COMMENT 'SHA-256 dạng hex của refresh token',
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6)     NULL,
    user_agent VARCHAR(255)    NULL,
    ip_address VARCHAR(45)     NULL COMMENT 'Đủ chỗ cho IPv6',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at) COMMENT 'Cho job dọn token hết hạn',
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Phiên đăng nhập còn hiệu lực';

-- Token dùng một lần: xác thực email và đặt lại mật khẩu
CREATE TABLE verification_tokens
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    type       VARCHAR(20) NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6)     NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_verification_tokens_hash (token_hash),
    KEY idx_verification_tokens_user (user_id, type),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_verification_tokens_type CHECK (type IN ('EMAIL_VERIFY', 'PASSWORD_RESET'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Token xác thực email / đặt lại mật khẩu';
