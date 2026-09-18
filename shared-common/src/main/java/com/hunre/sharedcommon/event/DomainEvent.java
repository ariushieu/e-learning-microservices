package com.hunre.sharedcommon.event;

import java.time.Instant;

/**
 * Hợp đồng chung cho mọi sự kiện đi qua Kafka.
 *
 * <p>Ba trường bắt buộc này là thứ hạ tầng cần, độc lập với nội dung nghiệp vụ:
 *
 * <ul>
 *   <li>{@link #eventId()} - mã UUID sinh đúng một lần tại nơi phát. Consumer ghi mã này
 *       vào bảng {@code processed_events} trước khi xử lý; trùng khóa chính nghĩa là sự
 *       kiện đã xử lý rồi, bỏ qua. Kafka bảo đảm at-least-once nên một sự kiện hoàn toàn
 *       có thể tới hai lần, và nếu không khử trùng lặp thì người học sẽ nhận hai email
 *       cho cùng một lần ghi danh.</li>
 *   <li>{@link #eventType()} - chuỗi định danh loại sự kiện, khớp với cột
 *       {@code outbox_events.event_type}. Xem {@link EventTypes}.</li>
 *   <li>{@link #occurredAt()} - thời điểm sự kiện xảy ra theo UTC. Đây là lúc nghiệp vụ
 *       xảy ra, không phải lúc message được đẩy lên Kafka; hai mốc này lệch nhau vì
 *       outbox gửi sau khi transaction đã commit.</li>
 * </ul>
 *
 * <p><b>Quy tắc sửa đổi.</b> Các lớp sự kiện là hợp đồng giữa hai service khác nhau,
 * không phải DTO nội bộ. Thêm trường mới thì không sao vì consumer cũ bỏ qua trường lạ.
 * Nhưng <b>đổi tên hoặc xóa trường là thay đổi phá vỡ</b>: consumer đang chạy sẽ đọc ra
 * null mà không báo lỗi gì. Cần đổi thì báo cả nhóm trước, đừng tự sửa.
 */
public interface DomainEvent {

    /** Mã UUID duy nhất của sự kiện, dùng để khử trùng lặp ở phía nhận. */
    String eventId();

    /** Loại sự kiện, xem {@link EventTypes}. */
    String eventType();

    /** Thời điểm nghiệp vụ xảy ra, theo UTC. */
    Instant occurredAt();
}
