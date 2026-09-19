-- =============================================================================
-- V2: Seed dữ liệu bản sao các khóa học (course_snapshots)
-- Đảm bảo đồng bộ chính xác số lượng bài học cho từng khóa học:
--   Khóa 1: 5 bài học (Kiến trúc Microservices với Spring Boot & Spring Cloud)
--   Khóa 2: 4 bài học (Thiết kế Cơ sở dữ liệu & Apache Kafka KRaft Mode)
--   Khóa 3: 3 bài học (Docker, Docker Compose & Triển khai Microservices)
-- =============================================================================

INSERT INTO course_snapshots (course_id, title, slug, instructor_id, total_lessons, synced_at)
VALUES 
    (1, 'Kiến trúc Microservices với Spring Boot & Spring Cloud', 'kien-truc-microservices-spring-boot', 1, 5, CURRENT_TIMESTAMP(6)),
    (2, 'Thiết kế Cơ sở dữ liệu & Apache Kafka (KRaft Mode)', 'thiet-ke-csdl-apache-kafka', 2, 4, CURRENT_TIMESTAMP(6)),
    (3, 'Docker, Docker Compose & Triển khai Microservices', 'docker-compose-trien-khai-microservices', 3, 3, CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE 
    title = VALUES(title),
    slug = VALUES(slug),
    instructor_id = VALUES(instructor_id),
    total_lessons = VALUES(total_lessons),
    synced_at = CURRENT_TIMESTAMP(6);
