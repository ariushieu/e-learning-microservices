package com.hunre.notificationservice.email;

import com.hunre.notificationservice.consumer.EventProcessor;
import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationPreference;
import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.entity.NotificationTemplate;
import com.hunre.notificationservice.repository.NotificationPreferenceRepository;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.NotificationTemplateRepository;
import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.notificationservice.repository.UserContactRepository;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.event.DomainEvent;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.event.UserProfileUpdatedEvent;
import com.hunre.sharedcommon.event.UserRegisteredEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Kênh email: dựng bản sao liên lạc từ auth-service, xếp hàng email, gửi và thử lại. */
@SpringBootTest
class EmailNotificationTest {
    @Autowired EventProcessor processor;
    @Autowired NotificationService service;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationTemplateRepository templates;
    @Autowired NotificationPreferenceRepository preferences;
    @Autowired ProcessedEventRepository processed;
    @Autowired UserContactRepository contacts;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        processed.deleteAll();
        templates.deleteAll();
        preferences.deleteAll();
        contacts.deleteAll();
        template("WELCOME", NotificationChannel.EMAIL, "Chào mừng", "Chào {fullName}, bắt đầu tại {url}");
        template("ENROLLMENT_SUCCESS", NotificationChannel.IN_APP, "Ghi danh thành công", "Khóa {courseTitle}");
        template("ENROLLMENT_SUCCESS", NotificationChannel.EMAIL, "Xác nhận ghi danh {courseTitle}",
                "Chào {fullName}, vào học: {url}");
    }

    private void template(String code, NotificationChannel channel, String title, String body) {
        templates.save(NotificationTemplate.builder().code(code).channel(channel)
                .titleTemplate(title).bodyTemplate(body).active(true).build());
    }

    private void process(DomainEvent event, String topic) {
        processor.process(event.eventId(), event.eventType(), topic, mapper.writeValueAsString(event));
    }

    private List<Notification> emails(long userId) {
        return notifications.findAll().stream()
                .filter(n -> n.getUserId() == userId && n.getChannel() == NotificationChannel.EMAIL).toList();
    }

    @Test
    void registrationSavesContactAndQueuesWelcomeEmailOutsideTheInbox() {
        process(UserRegisteredEvent.of(7L, "an@example.com", "Nguyễn An"), KafkaTopics.AUTH_EVENTS);

        assertThat(contacts.findById(7L)).hasValueSatisfying(c -> {
            assertThat(c.getEmail()).isEqualTo("an@example.com");
            assertThat(c.getFullName()).isEqualTo("Nguyễn An");
        });
        assertThat(emails(7L)).singleElement().satisfies(email -> {
            assertThat(email.getType()).isEqualTo("WELCOME");
            assertThat(email.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(email.getContent()).isEqualTo("Chào Nguyễn An, bắt đầu tại http://localhost:3000/courses");
        });
        assertThat(service.getMyNotifications(7L, PageRequest.of(0, 20)).content()).isEmpty();
        assertThat(service.countUnread(7L)).isZero();
    }

    @Test
    void profileUpdateOverwritesContactWithoutSendingMail() {
        process(UserRegisteredEvent.of(7L, "an@example.com", "Tên cũ"), KafkaTopics.AUTH_EVENTS);
        process(UserProfileUpdatedEvent.of(7L, "an@example.com", "Tên mới"), KafkaTopics.AUTH_EVENTS);
        assertThat(contacts.findById(7L)).hasValueSatisfying(c -> assertThat(c.getFullName()).isEqualTo("Tên mới"));
        assertThat(emails(7L)).hasSize(1);
    }

    @Test
    void businessEventSendsBothChannelsUnlessEmailIsTurnedOff() {
        process(UserProfileUpdatedEvent.of(7L, "an@example.com", "An"), KafkaTopics.AUTH_EVENTS);
        process(EnrollmentCreatedEvent.of(1L, 7L, 10L, "Java"), KafkaTopics.ENROLLMENT_EVENTS);
        assertThat(service.countUnread(7L)).isEqualTo(1);
        assertThat(emails(7L)).singleElement().satisfies(email -> {
            assertThat(email.getTitle()).isEqualTo("Xác nhận ghi danh Java");
            assertThat(email.getContent()).isEqualTo("Chào An, vào học: http://localhost:3000/learn/10");
        });

        preferences.save(NotificationPreference.builder().userId(8L).inAppEnabled(true).emailEnabled(false).build());
        process(EnrollmentCreatedEvent.of(2L, 8L, 10L, "Java"), KafkaTopics.ENROLLMENT_EVENTS);
        assertThat(service.countUnread(8L)).isEqualTo(1);
        assertThat(emails(8L)).isEmpty();
    }

    @Test
    void codesWithoutAnEmailTemplateQueueNothing() {
        assertThat(service.queueEmail("QUIZ_GRADED", 7L, null, "/attempts/1")).isEmpty();
    }

    private EmailDispatcher dispatcher(JavaMailSender sender) {
        var dispatcher = new EmailDispatcher(notifications, contacts, sender);
        ReflectionTestUtils.setField(dispatcher, "from", "E-Learning HUNRE <no-reply@hunre.edu.vn>");
        return dispatcher;
    }

    private JavaMailSender sender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        return sender;
    }

    @Test
    void dispatcherSendsToTheContactAndMarksSent() throws Exception {
        process(UserRegisteredEvent.of(7L, "an@example.com", "Nguyễn An"), KafkaTopics.AUTH_EVENTS);
        JavaMailSender sender = sender();
        dispatcher(sender).dispatchPending();

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0].toString()).isEqualTo("an@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Chào mừng");
        assertThat(emails(7L)).singleElement().satisfies(email -> {
            assertThat(email.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(email.getSentAt()).isNotNull();
        });
    }

    @Test
    void failuresAreRetriedThenGivenUp() {
        service.queueEmail("WELCOME", 9L, null, "/courses");   // chưa có contact
        JavaMailSender sender = sender();
        var dispatcher = dispatcher(sender);
        dispatcher.dispatchPending();
        assertThat(emails(9L)).singleElement().satisfies(email -> {
            assertThat(email.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(email.getRetryCount()).isEqualTo(1);
            assertThat(email.getLastError()).contains("Chưa có email");
        });
        verify(sender, never()).send(any(MimeMessage.class));

        // Contact tới sau nhưng máy chủ mail hỏng: thử tới đủ số lần rồi dừng.
        service.saveContact(9L, "late@example.com", "Đến muộn");
        doThrow(new MailSendException("SMTP down")).when(sender).send(any(MimeMessage.class));
        for (int i = 1; i < EmailDispatcher.MAX_ATTEMPTS + 2; i++) dispatcher.dispatchPending();
        assertThat(emails(9L)).singleElement().satisfies(email -> {
            assertThat(email.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(email.getRetryCount()).isEqualTo(EmailDispatcher.MAX_ATTEMPTS);
            assertThat(email.getLastError()).contains("SMTP down");
        });
        verify(sender, times(EmailDispatcher.MAX_ATTEMPTS - 1)).send(any(MimeMessage.class));
    }
}
