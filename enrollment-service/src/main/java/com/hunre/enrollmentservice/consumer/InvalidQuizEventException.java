package com.hunre.enrollmentservice.consumer;

/** Lỗi hợp đồng dữ liệu không thể sửa bằng cách đọc lại cùng message. */
public class InvalidQuizEventException extends RuntimeException {
    public InvalidQuizEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
