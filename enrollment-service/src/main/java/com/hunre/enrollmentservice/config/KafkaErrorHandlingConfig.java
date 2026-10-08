package com.hunre.enrollmentservice.config;

import com.hunre.enrollmentservice.consumer.InvalidCourseEventException;
import com.hunre.enrollmentservice.consumer.InvalidQuizEventException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

/** Retry hữu hạn; chỉ bỏ qua offset lỗi sau khi Kafka xác nhận đã lưu message trong DLT. */
@Configuration
@EnableConfigurationProperties(KafkaRetryProperties.class)
public class KafkaErrorHandlingConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);
    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    public DefaultErrorHandler courseSnapshotErrorHandler(KafkaOperations<?, ?> kafkaTemplate,
                                                          KafkaRetryProperties retry) {
        var deadLetter = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, -1));
        // Không coi recovery thành công nếu broker từ chối/không xác nhận bản sao DLT.
        deadLetter.setFailIfSendResultIsError(true);

        ConsumerAwareRecordRecoverer recoverer = (record, consumer, ex) -> {
            deadLetter.accept(record, consumer, ex);
            log.error("Đã chuyển message {}-{}@{} sang {}{} ({})",
                    record.topic(), record.partition(), record.offset(), record.topic(), DEAD_LETTER_SUFFIX,
                    ex.getCause() == null ? ex.getClass().getSimpleName() : ex.getCause().getClass().getSimpleName());
        };
        var backOff = new ExponentialBackOff(retry.initialInterval().toMillis(), retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());
        backOff.setMaxElapsedTime(retry.maxElapsedTime().toMillis());

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Một outage có thể đổi loại exception; không cấp lại ngân sách retry từ đầu.
        handler.setResetStateOnExceptionChange(false);
        handler.addNotRetryableExceptions(InvalidCourseEventException.class, InvalidQuizEventException.class, JacksonException.class,
                DataIntegrityViolationException.class);
        return handler;
    }
}
