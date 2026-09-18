package com.hunre.sharedcommon.dto;

import java.time.Instant;

/**
 * Vỏ bọc chung cho mọi response thành công của toàn hệ thống.
 *
 * <p>Mục đích là để frontend chỉ phải viết một hàm xử lý response duy nhất, thay vì
 * mỗi service trả về một hình dạng khác nhau. Response lỗi dùng {@link ErrorResponse},
 * phân biệt với response thành công bằng mã trạng thái HTTP.
 *
 * <p>Ví dụ trong controller:
 * <pre>{@code
 * @GetMapping("/{id}")
 * public ApiResponse<CourseResponse> findById(@PathVariable Long id) {
 *     return ApiResponse.ok(courseService.findById(id));
 * }
 * }</pre>
 *
 * @param success   luôn là true, để frontend kiểm tra nhanh mà không cần đọc mã HTTP
 * @param message   thông báo hiển thị cho người dùng, có thể null nếu không cần
 * @param data      dữ liệu trả về
 * @param timestamp thời điểm tạo response, theo UTC
 * @param <T>       kiểu dữ liệu trả về
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    /** Trả dữ liệu, không kèm thông báo. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, Instant.now());
    }

    /** Trả dữ liệu kèm thông báo hiển thị cho người dùng. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    /** Trả thông báo suông, dùng cho các thao tác không có dữ liệu trả về như xóa. */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }
}
