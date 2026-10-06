package com.hunre.enrollmentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Tổng ngân sách retry chỉ tính khoảng chờ, không tính thời gian truy cập database. */
@ConfigurationProperties("elearning.kafka.retry")
public record KafkaRetryProperties(
        @DefaultValue("1s") Duration initialInterval,
        @DefaultValue("2") double multiplier,
        @DefaultValue("30s") Duration maxInterval,
        @DefaultValue("5m") Duration maxElapsedTime) {

    public KafkaRetryProperties {
        if (initialInterval == null || initialInterval.toMillis() <= 0
                || maxInterval == null || maxInterval.compareTo(initialInterval) < 0
                || maxElapsedTime == null || maxElapsedTime.compareTo(initialInterval) < 0
                || !Double.isFinite(multiplier) || multiplier < 1) {
            throw new IllegalArgumentException("Kafka retry cần khoảng chờ dương, multiplier >= 1, "
                    + "maxInterval và maxElapsedTime >= initialInterval");
        }
    }
}
