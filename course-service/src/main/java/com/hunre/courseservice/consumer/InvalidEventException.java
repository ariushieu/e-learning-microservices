package com.hunre.courseservice.consumer;

/**
 * Message không xử lý được vì chính nội dung của nó hỏng, không phải vì hệ thống đang trục
 * trặc: JSON sai cú pháp, thiếu {@code eventId}, dữ liệu vi phạm ràng buộc của bảng.
 *
 * <p>Thử lại bao nhiêu lần cũng ra cùng một kết quả, nên bộ xử lý lỗi Kafka chuyển thẳng
 * message sang topic {@code .DLT} mà không chờ. Xem {@code KafkaErrorHandlingConfig}.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
