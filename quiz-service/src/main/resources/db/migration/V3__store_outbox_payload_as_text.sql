-- Preserve the exact serialized event, including decimal scale and key order.
-- Existing JSON rows keep their current representation; future events remain verbatim.
ALTER TABLE outbox_events MODIFY COLUMN payload LONGTEXT NOT NULL;
