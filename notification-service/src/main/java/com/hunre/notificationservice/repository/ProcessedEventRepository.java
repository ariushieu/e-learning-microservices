package com.hunre.notificationservice.repository;

import com.hunre.notificationservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    /**
     * Ghi sổ một sự kiện, ném lỗi nếu nó đã có trong sổ.
     *
     * <p><b>Vì sao không dùng {@code save()}.</b> Spring Data quyết định INSERT hay UPDATE
     * dựa vào {@code @Id} có null hay không. Khóa ở đây là UUID do bên gửi sinh ra nên luôn
     * khác null, thành ra {@code save()} hiểu là "đã tồn tại" và gọi {@code merge()} — tức
     * là UPDATE đè lên dòng cũ, không hề có lỗi trùng khóa. Cơ chế chống trùng sẽ im lặng
     * mất tác dụng và người dùng nhận hai thông báo giống hệt nhau.
     *
     * <p>Câu INSERT tường minh thì luôn là INSERT, nên ràng buộc khóa chính làm đúng việc
     * của nó: sự kiện đã xử lý rồi thì ném {@code DataIntegrityViolationException}.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException khi {@code eventId}
     *         đã có trong sổ
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, event_type, source_topic, processed_at)
            VALUES (:eventId, :eventType, :sourceTopic, :processedAt)
            """, nativeQuery = true)
    void insertNew(@Param("eventId") String eventId,
                   @Param("eventType") String eventType,
                   @Param("sourceTopic") String sourceTopic,
                   @Param("processedAt") Instant processedAt);
}
