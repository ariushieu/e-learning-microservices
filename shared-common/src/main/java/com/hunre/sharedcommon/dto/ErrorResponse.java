package com.hunre.sharedcommon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Hình dạng thống nhất của mọi response lỗi trong hệ thống.
 *
 * <p>Được {@code GlobalExceptionHandler} tạo ra tự động, service không cần tự dựng.
 * Nhờ vậy 5 service trả lỗi giống hệt nhau và frontend chỉ viết một chỗ xử lý.
 *
 * <p>Ví dụ lỗi kiểm tra dữ liệu:
 * <pre>{@code
 * {
 *   "success": false,
 *   "code": "VALIDATION_FAILED",
 *   "message": "Dữ liệu gửi lên không hợp lệ",
 *   "path": "/api/courses",
 *   "timestamp": "2026-09-18T10:12:33.123Z",
 *   "fieldErrors": [
 *     { "field": "title", "message": "không được để trống" }
 *   ]
 * }
 * }</pre>
 *
 * @param success     luôn là false
 * @param code        mã lỗi dạng chữ, xem {@code ErrorCode}. Frontend nên xử lý theo mã
 *                    này chứ đừng so sánh chuỗi {@code message}
 * @param message     mô tả lỗi để hiển thị cho người dùng
 * @param path        đường dẫn đã gọi
 * @param timestamp   thời điểm xảy ra lỗi, theo UTC
 * @param fieldErrors chi tiết từng trường sai, bị lược khỏi JSON khi rỗng
 */
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        String path,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDetail> fieldErrors
) {

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(false, code, message, path, Instant.now(), List.of());
    }

    public static ErrorResponse of(String code, String message, String path, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                false,
                code,
                message,
                path,
                Instant.now(),
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors));
    }
}
