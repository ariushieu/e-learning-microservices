package com.hunre.sharedcommon.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình xác thực dùng chung cho mọi service, đặt dưới tiền tố {@code elearning.security}.
 *
 * <pre>{@code
 * elearning.security.enabled=true
 * elearning.security.jwt-secret=${JWT_SECRET}
 * elearning.security.public-paths=/actuator/**,/api/courses/**
 * }</pre>
 */
@ConfigurationProperties(prefix = "elearning.security")
public class JwtSecurityProperties {

    /**
     * Bật kiểm token cho service này.
     *
     * <p>Mặc định bật. Tắt trong profile dev để gọi Postman không cần token, giống cách
     * quiz-service tắt Kafka. Đừng tắt ở profile mặc định: lúc đó service tin mọi request.
     */
    private boolean enabled = true;

    /**
     * Khóa ký HS256, phải trùng với {@code jwt.secret} của auth-service, tối thiểu 32 ký tự.
     * Để trống khi {@code enabled=false}.
     */
    private String jwtSecret;

    /**
     * Đường dẫn không cần token, theo cú pháp Ant của Spring ({@code /api/courses/**}).
     *
     * <p>Mặc định đã mở sẵn actuator và ba endpoint đăng nhập của auth-service — nếu
     * chúng cũng đòi token thì không ai lấy được token đầu tiên. Service nào muốn mở thêm
     * endpoint đọc công khai thì khai lại danh sách này, khai lại là <b>thay thế</b> chứ
     * không phải cộng thêm.
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh-token"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? new ArrayList<>() : publicPaths;
    }

    /** Danh tính giả lập dùng khi {@code enabled=false}. Xem {@link DevUser}. */
    private DevUser devUser = new DevUser();

    public DevUser getDevUser() {
        return devUser;
    }

    public void setDevUser(DevUser devUser) {
        this.devUser = devUser == null ? new DevUser() : devUser;
    }

    /**
     * Người dùng giả lập cho lúc phát triển.
     *
     * <p>Khi tắt xác thực, controller vẫn cần biết "ai đang gọi" để lưu đúng chủ dữ liệu.
     * Nếu không có danh tính giả lập thì mọi endpoint nhận {@link AuthenticatedUser} sẽ
     * hỏng ngay khi tắt xác thực, và cái công tắc dev thành vô dụng.
     *
     * <p>Chỉ có tác dụng khi {@code elearning.security.enabled=false}. Gửi kèm token thật
     * thì token thật vẫn được ưu tiên.
     */
    public static class DevUser {

        private Long id = 1L;
        private String email = "dev@hunre.edu.vn";
        private String fullName = "Tài khoản dev";

        /** Mặc định đủ cả ba vai trò để thử endpoint nào cũng được. */
        private List<String> roles = new ArrayList<>(List.of(
                Roles.STUDENT, Roles.INSTRUCTOR, Roles.ADMIN));

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? new ArrayList<>() : roles;
        }
    }
}
