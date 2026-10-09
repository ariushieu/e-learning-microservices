package com.hunre.notificationservice.email;

import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.entity.UserContact;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.UserContactRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Gửi các email đang chờ (dòng notifications kênh EMAIL, trạng thái PENDING).
 *
 * <p>Tách khỏi lúc xử lý sự kiện Kafka: máy chủ mail chậm hay hỏng thì email nằm chờ, sự kiện
 * vẫn xử lý xong. Mỗi lần gửi hỏng tăng {@code retry_count} và ghi {@code last_error}; hỏng đủ
 * {@link #MAX_ATTEMPTS} lần thì chuyển FAILED và thôi thử. Chưa có email người nhận (sự kiện của
 * auth-service tới sau) cũng tính là một lần hỏng, để lượt sau thử lại.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elearning.mail.enabled", havingValue = "true", matchIfMissing = true)
public class EmailDispatcher {

    static final int MAX_ATTEMPTS = 5;
    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final NotificationRepository notifications;
    private final UserContactRepository contacts;
    private final JavaMailSender mailSender;

    @Value("${elearning.mail.from:E-Learning HUNRE <no-reply@hunre.edu.vn>}")
    private String from;

    @Scheduled(fixedDelayString = "${elearning.mail.dispatch-delay-ms:5000}")
    public void dispatchPending() {
        for (Notification email : notifications.findTop20ByChannelAndStatusOrderByIdAsc(
                NotificationChannel.EMAIL, NotificationStatus.PENDING)) {
            dispatch(email);
        }
    }

    void dispatch(Notification email) {
        Optional<UserContact> contact = contacts.findById(email.getUserId());
        try {
            if (contact.isEmpty()) {
                throw new IllegalStateException("Chưa có email của người dùng " + email.getUserId());
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(contact.get().getEmail());
            helper.setSubject(email.getTitle());
            helper.setText(email.getContent(), false);
            mailSender.send(message);

            email.setStatus(NotificationStatus.SENT);
            email.setSentAt(Notification.now());
            email.setLastError(null);
            log.info("Đã gửi email {} id={} cho người dùng {}", email.getType(), email.getId(), email.getUserId());
        } catch (Exception ex) {
            int attempts = email.getRetryCount() + 1;
            email.setRetryCount(attempts);
            email.setLastError(truncate(ex.getMessage()));
            if (attempts >= MAX_ATTEMPTS) {
                email.setStatus(NotificationStatus.FAILED);
            }
            log.warn("Gửi email id={} lần {} hỏng: {}", email.getId(), attempts, ex.getMessage());
        }
        notifications.save(email);
    }

    private static String truncate(String message) {
        if (message == null) return "Lỗi không rõ";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
