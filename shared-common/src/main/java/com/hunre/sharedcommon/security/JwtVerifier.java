package com.hunre.sharedcommon.security;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Kiểm chữ ký JWT rồi đọc danh tính người dùng ra khỏi token.
 *
 * <p>Chỉ làm đúng việc xác thực, không đụng gì tới HTTP, nên dùng được cả ở service chạy
 * Spring MVC lẫn ở api-gateway chạy WebFlux.
 *
 * <p><b>Vì sao service tự kiểm chứ không tin gateway.</b> Nếu service tin vào header kiểu
 * {@code X-User-Id} do gateway gắn vào, thì bất kỳ ai gọi thẳng {@code localhost:8082} kèm
 * header tự chế đều trở thành người khác. Tự kiểm chữ ký thì muốn giả mạo phải có khóa ký.
 *
 * <p>Token do auth-service phát hành bằng HMAC với khóa đối xứng dùng chung. Thuật toán
 * cụ thể do jjwt chọn theo độ dài khóa: từ 64 ký tự trở lên là HS512, từ 48 là HS384, từ
 * 32 là HS256. Khóa mặc định lúc phát triển dài 78 ký tự nên thực tế đang chạy HS512.
 * Bên kiểm không cần khai thuật toán, chỉ cần đúng khóa.
 *
 * <p>Khóa đối xứng nghĩa là mọi service đều có khả năng <i>ký</i> token chứ không chỉ
 * kiểm. Chấp nhận được với sản phẩm môn học; hệ thống thật nên chuyển sang cặp khóa bất
 * đối xứng (RS256) để chỉ auth-service giữ khóa riêng.
 */
public class JwtVerifier {

    /** Ngưỡng thấp nhất jjwt chấp nhận: HS256 cần khóa 256 bit, tức 32 ký tự ASCII. */
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey signingKey;

    public JwtVerifier(String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "elearning.security.jwt-secret phải có ít nhất " + MIN_SECRET_LENGTH
                            + " ký tự, đang có "
                            + (secret == null ? 0 : secret.length()));
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Kiểm token và trả về người dùng bên trong.
     *
     * <p>Chữ ký sai, token hết hạn, token bị sửa hay không đọc được đều ném cùng một lỗi
     * với cùng một thông báo. Cố ý như vậy: nói rõ "chữ ký sai" hay "token hết hạn" là
     * giúp người tấn công biết họ đang sai ở đâu.
     *
     * @throws BusinessException mã {@link ErrorCode#UNAUTHORIZED} nếu token không hợp lệ
     */
    public AuthenticatedUser verify(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ", ex);
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ", ex);
        }

        return new AuthenticatedUser(
                userId,
                claims.get("email", String.class),
                claims.get("fullName", String.class),
                readRoles(claims));
    }

    /**
     * Claim {@code roles} được jjwt trả về dạng {@code List} chứ không phải {@code Set},
     * và phần tử bên trong chỉ chắc chắn là {@code Object}. Lọc lấy chuỗi để một token
     * dị dạng không làm cả filter nổ.
     */
    private Set<String> readRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (!(raw instanceof Collection<?> values)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof String role && !role.isBlank()) {
                roles.add(role);
            }
        }
        return roles;
    }
}
