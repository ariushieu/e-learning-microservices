package com.hunre.sharedcommon.exception;

/**
 * Tài nguyên đã tồn tại. Trả về HTTP 409 với mã {@link ErrorCode#DUPLICATE_RESOURCE}.
 *
 * <p>Dùng cho email đăng ký trùng, slug khóa học trùng, ghi danh hai lần cùng một khóa.
 * Lưu ý vẫn phải giữ ràng buộc UNIQUE dưới database: kiểm tra ở tầng service không chặn
 * được hai request gửi lên cùng lúc.
 *
 * <pre>{@code
 * if (userRepository.existsByEmail(email)) {
 *     throw new DuplicateResourceException("người dùng", "email", email);
 * }
 * }</pre>
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    /**
     * Dựng sẵn thông báo dạng "người dùng với email = a@b.c đã tồn tại".
     *
     * @param resourceName tên tài nguyên bằng tiếng Việt
     * @param fieldName    tên trường bị trùng
     * @param value        giá trị bị trùng
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object value) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                "%s với %s = %s đã tồn tại".formatted(resourceName, fieldName, value));
    }
}
