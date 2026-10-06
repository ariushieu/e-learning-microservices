package com.hunre.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Cách thử lại khi xử lý một sự kiện Kafka bị lỗi, đọc từ {@code elearning.kafka.retry.*}.
 *
 * <p>Khoảng chờ tăng gấp {@code multiplier} sau mỗi lần, không quá {@code maxInterval}. Tổng thời
 * gian chờ vượt {@code maxElapsedTime} mà vẫn lỗi thì bỏ cuộc và chuyển message sang topic
 * {@code .DLT}.
 *
 * @param initialInterval chờ bao lâu trước lần thử lại đầu tiên
 * @param multiplier      mỗi lần sau chờ gấp mấy lần lần trước
 * @param maxInterval     khoảng chờ dài nhất giữa hai lần thử
 * @param maxElapsedTime  tổng thời gian chờ giữa các lần thử trước khi bỏ cuộc. Không tính
 *                        thời gian của chính các lần thử, nên thời gian thật dài hơn
 */
@ConfigurationProperties("elearning.kafka.retry")
public record KafkaRetryProperties(
        @DefaultValue("1s") Duration initialInterval,
        @DefaultValue("2") double multiplier,
        @DefaultValue("30s") Duration maxInterval,
        @DefaultValue("5m") Duration maxElapsedTime) {
}
