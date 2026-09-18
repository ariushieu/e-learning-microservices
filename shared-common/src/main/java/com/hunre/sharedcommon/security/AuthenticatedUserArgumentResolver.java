package com.hunre.sharedcommon.security;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cho phép controller nhận thẳng {@link AuthenticatedUser} làm tham số.
 *
 * <p>Trước:
 * <pre>{@code
 * @PostMapping("/{quizId}/attempts")
 * ApiResponse<X> start(@PathVariable Long quizId, @RequestParam Long userId) { ... }
 * }</pre>
 *
 * <p>Sau:
 * <pre>{@code
 * @PostMapping("/{quizId}/attempts")
 * ApiResponse<X> start(@PathVariable Long quizId, AuthenticatedUser user) {
 *     ... user.userId() ...
 * }
 * }</pre>
 *
 * <p>Khác biệt không nằm ở chỗ gõ ít hơn: {@code ?userId=5} là do client tự khai nên ai
 * cũng làm bài hộ người khác được, còn {@code user.userId()} đến từ token đã kiểm chữ ký.
 */
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object user = request == null ? null : request.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE);

        if (user instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        // Tới được đây nghĩa là endpoint nằm trong public-paths nhưng lại đòi danh tính,
        // hoặc xác thực đang tắt. Trả 401 thay vì null để không có chỗ nào lặng lẽ nhận
        // một người dùng rỗng rồi ghi dữ liệu sai chủ.
        throw new BusinessException(ErrorCode.UNAUTHORIZED,
                "Endpoint này cần đăng nhập nhưng đang được khai là công khai");
    }
}
