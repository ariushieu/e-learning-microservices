package com.hunre.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Sổ ghi những sự kiện Kafka đã xử lý, để không xử lý lại lần thứ hai.
 *
 * <p>Kafka bảo đảm at-least-once: consumer nhận xong rồi chết trước khi commit offset thì
 * lần sau nhận lại đúng sự kiện đó. Không có bảng này thì học viên nhận hai thông báo
 * giống hệt nhau cho cùng một lần nộp bài.
 *
 * <p>{@code event_id} vừa là khóa chính vừa là cơ chế khóa: chèn trước khi xử lý, trùng
 * khóa nghĩa là đã xử lý rồi. Dựa vào ràng buộc của database thay vì kiểm tra trước rồi
 * mới ghi, vì hai consumer chạy song song có thể cùng vượt qua bước kiểm tra đó.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    /** UUID do service phát sự kiện sinh ra, lấy từ trường eventId trong message. */
    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "source_topic", length = 100)
    private String sourceTopic;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
