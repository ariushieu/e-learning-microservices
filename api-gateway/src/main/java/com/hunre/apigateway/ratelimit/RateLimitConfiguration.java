package com.hunre.apigateway.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nạp ba chính sách giới hạn vào {@link RedisRateLimiter} mà Spring Cloud Gateway tự tạo
 * khi có Redis trong classpath, rồi dựng filter dùng nó.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    RateLimitGatewayFilter rateLimitGatewayFilter(RedisRateLimiter redisRateLimiter,
                                                  RateLimitProperties properties) {
        register(redisRateLimiter, RateLimitGatewayFilter.LOGIN_POLICY, "login", properties.login());
        register(redisRateLimiter, RateLimitGatewayFilter.LOGIN_IP_POLICY, "login-ip", properties.loginIp());
        register(redisRateLimiter, RateLimitGatewayFilter.API_POLICY, "api", properties.api());
        return new RateLimitGatewayFilter(redisRateLimiter, properties);
    }

    private static void register(RedisRateLimiter limiter, String policyId, String name,
                                 RateLimitProperties.Policy policy) {
        // Sai cấu hình thì dừng ngay lúc khởi động, đừng để tới lúc có request mới lộ: thiếu
        // chính sách thì RedisRateLimiter ném lỗi ở mọi request, còn mỗi request lấy nhiều
        // token hơn sức chứa của xô thì không request nào lọt qua được.
        if (policy == null) {
            throw new IllegalStateException(
                    "Thiếu cấu hình elearning.rate-limit.%s.*".formatted(name));
        }
        if (policy.replenishRate() < 1 || policy.requestedTokens() < 1
                || policy.requestedTokens() > policy.burstCapacity()) {
            throw new IllegalStateException(("elearning.rate-limit.%s không hợp lệ: cần "
                    + "replenish-rate >= 1, requested-tokens >= 1 và requested-tokens <= "
                    + "burst-capacity, đang là %s").formatted(name, policy));
        }

        limiter.getConfig().put(policyId, new RedisRateLimiter.Config()
                .setReplenishRate(policy.replenishRate())
                .setBurstCapacity(policy.burstCapacity())
                .setRequestedTokens(policy.requestedTokens()));
    }
}
