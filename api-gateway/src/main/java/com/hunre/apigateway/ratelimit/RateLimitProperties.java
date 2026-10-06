package com.hunre.apigateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình giới hạn request, đọc từ {@code elearning.rate-limit.*} trong
 * {@code application.properties}.
 *
 * <p>Mỗi chính sách là một xô token (token bucket): xô chứa tối đa {@code burstCapacity}
 * token, mỗi giây được nạp thêm {@code replenishRate} token, mỗi request lấy đi
 * {@code requestedTokens} token. Xô cạn thì request nhận 429. Muốn giới hạn theo phút thì
 * cho mỗi request lấy nhiều token: nạp 1 token/giây, mỗi request lấy 6 token là được 10
 * request mỗi phút.
 *
 * @param enabled tắt hẳn việc giới hạn, dùng khi chạy thử tải hoặc khi cần loại trừ nguyên
 *                nhân lúc gỡ lỗi
 * @param login   chính sách cho đăng nhập, đăng ký, làm mới token — tính theo địa chỉ IP
 * @param api     chính sách cho mọi đường dẫn còn lại — tính theo người dùng nếu đã đăng
 *                nhập, theo địa chỉ IP nếu chưa
 */
@ConfigurationProperties("elearning.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        Policy login,
        Policy api) {

    public record Policy(int replenishRate, int burstCapacity, @DefaultValue("1") int requestedTokens) {

        /**
         * Số giây phải chờ để xô có lại đủ token cho một request. Gửi cho client qua header
         * {@code Retry-After} để frontend biết lúc nào thử lại, thay vì bấm liên tục.
         */
        public long retryAfterSeconds() {
            return Math.max(1, (requestedTokens + replenishRate - 1) / replenishRate);
        }
    }
}
