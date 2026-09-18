package com.hunre.sharedcommon.security;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Đây là phần duy nhất quyết định ai được làm gì trong cả hệ thống, nên test tập trung vào
 * chuyện nó phải <b>từ chối</b> đúng, chứ không chỉ chấp nhận đúng.
 */
class JwtVerifierTest {

    private static final String SECRET =
            "elearning-microservices-auth-service-jwt-secret-key-for-local-dev-only-32bytes";
    private static final String SECRET_KHAC =
            "mot-khoa-hoan-toan-khac-nhung-cung-du-dai-de-jjwt-chap-nhan-duoc-nhe-ban-oi";

    private final JwtVerifier verifier = new JwtVerifier(SECRET);

    private String tokenKyBang(String secret, Instant hetHan, Object roles) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("42")
                .claim("email", "sv@hunre.edu.vn")
                .claim("fullName", "Nguyễn Văn A")
                .claim("roles", roles)
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(hetHan))
                .signWith(key)
                .compact();
    }

    private String tokenHopLe() {
        return tokenKyBang(SECRET, Instant.now().plusSeconds(900), List.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("token hợp lệ đọc ra đúng id, email, tên và vai trò")
    void docDungThongTinTuToken() {
        AuthenticatedUser user = verifier.verify(tokenHopLe());

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("sv@hunre.edu.vn");
        assertThat(user.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(user.roles()).containsExactly(Roles.STUDENT);
    }

    @Test
    @DisplayName("token ký bằng khóa khác bị từ chối")
    void chuKySai() {
        String token = tokenKyBang(SECRET_KHAC, Instant.now().plusSeconds(900), List.of(Roles.ADMIN));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token không hợp lệ");
    }

    @Test
    @DisplayName("token hết hạn bị từ chối với thông báo riêng để người dùng biết đăng nhập lại")
    void tokenHetHan() {
        String token = tokenKyBang(SECRET, Instant.now().minusSeconds(10), List.of(Roles.STUDENT));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Phiên đăng nhập đã hết hạn");
    }

    @Test
    @DisplayName("sửa nội dung token thì chữ ký không còn khớp")
    void tokenBiSua() {
        // Sửa phần payload chứ không sửa ký tự cuối chữ ký: ký tự base64url cuối cùng
        // mang vài bit đệm không được dùng tới, đổi nó thì chữ ký vẫn hợp lệ.
        String[] phan = tokenHopLe().split("\\.");
        char[] payload = phan[1].toCharArray();
        int giua = payload.length / 2;
        payload[giua] = payload[giua] == 'a' ? 'b' : 'a';

        String suaDoi = phan[0] + "." + new String(payload) + "." + phan[2];

        assertThatThrownBy(() -> verifier.verify(suaDoi))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("mọi lỗi token đều trả mã UNAUTHORIZED, không phải lỗi 500")
    void luonLaUnauthorized() {
        assertThatThrownBy(() -> verifier.verify("khong-phai-jwt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("thông báo lỗi không tiết lộ token sai ở chỗ nào")
    void khongTietLoChiTiet() {
        assertThatThrownBy(() -> verifier.verify(
                tokenKyBang(SECRET_KHAC, Instant.now().plusSeconds(900), List.of())))
                .hasMessageNotContainingAny("signature", "chữ ký", "HMAC", "key");
    }

    @Test
    @DisplayName("token không có claim roles vẫn đọc được, chỉ là không có vai trò nào")
    void thieuRoles() {
        AuthenticatedUser user = verifier.verify(
                tokenKyBang(SECRET, Instant.now().plusSeconds(900), null));

        assertThat(user.roles()).isEmpty();
        assertThat(user.hasRole(Roles.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("claim roles chứa dữ liệu lạ thì bỏ qua phần lạ chứ không làm hỏng cả request")
    void rolesDiDang() {
        AuthenticatedUser user = verifier.verify(
                tokenKyBang(SECRET, Instant.now().plusSeconds(900),
                        List.of(Roles.STUDENT, 123, "")));

        assertThat(user.roles()).containsExactly(Roles.STUDENT);
    }

    @Test
    @DisplayName("subject không phải số bị từ chối, không để lọt userId rác")
    void subjectKhongPhaiSo() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("khong-phai-so")
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token không hợp lệ");
    }

    @Test
    @DisplayName("khóa quá ngắn bị chặn ngay lúc khởi động chứ không đợi tới request đầu tiên")
    void khoaQuaNgan() {
        assertThatThrownBy(() -> new JwtVerifier("ngan-qua"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ít nhất 32");
    }

    @Test
    @DisplayName("khóa null cũng bị chặn, tránh service chạy mà không thực sự kiểm gì")
    void khoaNull() {
        assertThatThrownBy(() -> new JwtVerifier(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("hasAnyRole đúng với cả trường hợp có và không có")
    void kiemTraVaiTro() {
        AuthenticatedUser user = verifier.verify(
                tokenKyBang(SECRET, Instant.now().plusSeconds(900),
                        List.of(Roles.STUDENT, Roles.INSTRUCTOR)));

        assertThat(user.hasAnyRole(Roles.ADMIN, Roles.INSTRUCTOR)).isTrue();
        assertThat(user.hasAnyRole(Roles.ADMIN)).isFalse();
        assertThat(user.hasRole(Roles.STUDENT)).isTrue();
    }
}
