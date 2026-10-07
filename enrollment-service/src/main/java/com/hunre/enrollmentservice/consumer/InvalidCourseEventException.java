package com.hunre.enrollmentservice.consumer;

/** Message không thể xử lý bằng retry; giữ lại trong DLT để kiểm tra/sửa dữ liệu. */
public class InvalidCourseEventException extends RuntimeException {
    public InvalidCourseEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
