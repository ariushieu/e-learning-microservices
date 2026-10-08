package com.hunre.enrollmentservice.consumer;

/** Chỉ dùng cho trùng khóa event_id trong sổ nhận sự kiện, không nuốt lỗi constraint nghiệp vụ. */
public class DuplicateQuizEventException extends RuntimeException {
    public DuplicateQuizEventException(Throwable cause) {
        super("Sự kiện quiz đã được xử lý", cause);
    }
}
