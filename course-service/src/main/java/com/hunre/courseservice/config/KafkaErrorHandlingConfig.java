package com.hunre.courseservice.config;

import com.hunre.courseservice.consumer.InvalidEventException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

/** Thử lại lỗi tạm thời có giới hạn; giữ message hỏng trong topic gốc + .DLT. */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KafkaRetryProperties.class)
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate,
                                                 KafkaRetryProperties retry) {

        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(kafkaTemplate,
                // Partition -1: để Kafka tự chọn. Mặc định Spring gửi vào đúng số partition
                // của message gốc, và hỏng nếu topic .DLT ít partition hơn topic gốc.
                (record, ex) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, -1));

        // Không coi là xử lý xong nếu broker chưa nhận được bản sao giữ trong DLT.
        deadLetter.setFailIfSendResultIsError(true);

        // Spring chuyển sang .DLT mà không ghi dòng log nào, nên người vận hành không biết có
        // message vừa bị loại. Ghi một dòng ERROR ngắn; chi tiết đầy đủ đã nằm trong header
        // kafka_dlt-exception-stacktrace của chính message đó.
        ConsumerAwareRecordRecoverer recoverer = (record, consumer, ex) -> {
            log.error("Chuyển message {}-{}@{} sang {}{}: {}",
                    record.topic(), record.partition(), record.offset(),
                    record.topic(), DEAD_LETTER_SUFFIX,
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            deadLetter.accept(record, consumer, ex);
        };

        ExponentialBackOff backOff = new ExponentialBackOff(
                retry.initialInterval().toMillis(), retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());
        backOff.setMaxElapsedTime(retry.maxElapsedTime().toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Mặc định Spring đã coi lỗi chuyển kiểu, lỗi deserialize là không thử lại. Thêm hai
        // loại của riêng service này: message hỏng do consumer phát hiện, và JSON không
        // khớp lớp sự kiện khi consumer đọc chi tiết.
        handler.addNotRetryableExceptions(InvalidEventException.class, JacksonException.class);
        return handler;
    }
}
