-- Fifteen digits, fourteen separators and an optional leading plus sign.
ALTER TABLE users MODIFY COLUMN phone VARCHAR(30) NULL;
