-- Check the final keys before changing any user. A duplicate aborts Flyway with
-- uk_normalized_email_collision; resolve the conflicting accounts manually.
-- Do not use IGNORE/REPLACE: neither account may be discarded or merged.
CREATE TEMPORARY TABLE auth_email_normalization_check
(
    email VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    UNIQUE KEY uk_normalized_email_collision (email)
);

INSERT INTO auth_email_normalization_check (email)
SELECT LOWER(TRIM(email)) FROM users;

-- Switch comparison first: otherwise an accent-insensitive old unique index can
-- reject distinct emails while trimming a legacy address with leading spaces.
ALTER TABLE users
    MODIFY email VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

UPDATE users SET email = LOWER(TRIM(email));

DROP TEMPORARY TABLE auth_email_normalization_check;
