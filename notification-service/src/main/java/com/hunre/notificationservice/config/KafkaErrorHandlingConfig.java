package com.hunre.notificationservice.config;

import com.hunre.notificationservice.consumer.InvalidEventException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

/**
 * Quyết định làm gì với một sự kiện Kafka xử lý bị lỗi, để không sự kiện nào mất im lặng.
 *
 * <p>Có hai loại lỗi và chúng cần cách xử lý ngược nhau:
 *
 * <ul>
 *   <li><b>Lỗi tạm thời</b> — MySQL khởi động lại, mất kết nối, hết connection. Message
 *       không có gì sai, đợi một lúc là xử lý được. Thử lại với khoảng chờ tăng dần (1s,
 *       2s, 4s… tối đa 30s). Trong lúc đó consumer đứng chờ ở message
 *       này chứ không nhảy qua, nên thứ tự sự kiện của từng người dùng vẫn giữ nguyên.</li>
 *   <li><b>Message hỏng</b> — JSON sai, thiếu trường, dữ liệu vi phạm ràng buộc. Thử lại
 *       bao nhiêu lần cũng vậy, chờ chỉ làm kẹt mọi sự kiện phía sau. Chuyển thẳng sang
 *       topic {@code .DLT} ngay lần đầu.</li>
 * </ul>
 *
 * <p>Tổng thời gian <i>chờ giữa các lần</i> vượt 5 phút mà vẫn lỗi thì message cũng sang
 * {@code .DLT}. Lưu ý Spring chỉ cộng thời gian chờ, không cộng thời gian mỗi lần thử bị treo:
 * MySQL tắt hẳn thì mỗi lần thử đợi kết nối thêm khoảng 30 giây, nên thực tế khoảng 10–12
 * phút mới bỏ cuộc. Không thử mãi được: một lỗi lập trình mà bị đoán nhầm là lỗi tạm thời sẽ
 * chặn đứng consumer.
 *
 * <p><b>Topic {@code .DLT}</b> là nơi giữ lại message để người xem lại được, tên là topic gốc
 * cộng đuôi {@code .DLT}, ví dụ {@code elearning.quiz.events.DLT}. Mở trong Kafka UI sẽ thấy
 * nguyên nội dung message kèm các header {@code kafka_dlt-exception-message} và
 * {@code kafka_dlt-original-offset} cho biết vì sao hỏng và nó nằm ở đâu trong topic gốc.
 * Topic tự được tạo lần đầu có message, nhờ {@code KAFKA_AUTO_CREATE_TOPICS_ENABLE}.
 *
 * <p>Spring Boot tự gắn bean {@code CommonErrorHandler} này vào listener container, nên
 * {@code KafkaEventConsumer} không cần khai gì thêm.
 */
@Configuration
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
        // khớp lớp sự kiện khi EventProcessor đọc chi tiết.
        handler.addNotRetryableExceptions(InvalidEventException.class, JacksonException.class);
        return handler;
    }
}
