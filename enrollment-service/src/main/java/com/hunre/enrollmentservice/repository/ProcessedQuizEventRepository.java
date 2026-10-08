package com.hunre.enrollmentservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class ProcessedQuizEventRepository {
    private final JdbcTemplate jdbc;

    /** INSERT thật: khóa chính quyết định người thắng khi hai consumer nhận cùng eventId. */
    public void insert(String eventId) {
        jdbc.update("INSERT INTO processed_quiz_events (event_id, processed_at) VALUES (?, ?)",
                eventId, Timestamp.from(Instant.now()));
    }
}
