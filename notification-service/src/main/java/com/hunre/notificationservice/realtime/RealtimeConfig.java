package com.hunre.notificationservice.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

/**
 * Chọn cách phát theo {@code elearning.realtime.redis-enabled}. Bật lịch chạy cho nhịp giữ
 * kết nối của {@link SseHub}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(RealtimeProperties.class)
public class RealtimeConfig {

    private static final String REDIS_ENABLED = "elearning.realtime.redis-enabled";

    @Bean
    @ConditionalOnProperty(name = REDIS_ENABLED, havingValue = "true", matchIfMissing = true)
    RedisRealtimeBroadcaster redisRealtimeBroadcaster(
            StringRedisTemplate redis, SseHub hub, ObjectMapper objectMapper) {
        return new RedisRealtimeBroadcaster(redis, hub, objectMapper);
    }

    /** Nghe kênh Redis; Redis chưa lên lúc khởi động thì tự thử lại, xem RedisSubscription. */
    @Bean
    @ConditionalOnProperty(name = REDIS_ENABLED, havingValue = "true", matchIfMissing = true)
    RedisSubscription realtimeRedisSubscription(
            RedisConnectionFactory connectionFactory, RedisRealtimeBroadcaster broadcaster) {
        return new RedisSubscription(connectionFactory, broadcaster);
    }

    /** Chạy một bản hoặc không có Redis: gửi thẳng cho kết nối tại chỗ. */
    @Bean
    @ConditionalOnProperty(name = REDIS_ENABLED, havingValue = "false")
    RealtimeBroadcaster localRealtimeBroadcaster(SseHub hub) {
        return hub::send;
    }
}
