package com.hunre.sharedcommon.exception;

/**
 * Không tìm thấy tài nguyên. Trả về HTTP 404 với mã {@link ErrorCode#RESOURCE_NOT_FOUND}.
 *
 * <pre>{@code
 * courseRepository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));
 * }</pre>
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * Dựng sẵn thông báo dạng "Không tìm thấy khóa học với id = 5".
     *
     * @param resourceName tên tài nguyên bằng tiếng Việt, ví dụ {@code "khóa học"}
     * @param fieldName    tên tiêu chí tìm kiếm, ví dụ {@code "id"}
     * @param value        giá trị đã tìm
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                "Không tìm thấy %s với %s = %s".formatted(resourceName, fieldName, value));
    }
}
