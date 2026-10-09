package com.hunre.authservice.outbox;

import com.hunre.authservice.dto.RegisterRequest;
import com.hunre.authservice.dto.UpdateProfileRequest;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:account_events;MODE=MySQL;DB_CLOSE_DELAY=-1")
class AccountEventsIntegrationTest {
    @Autowired AuthService auth;
    @Autowired OutboxEventRepository outbox;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
    }

    private RegisterRequest registration(String email, String name) {
        var request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Student@123456");
        request.setFullName(name);
        return request;
    }

    private UpdateProfileRequest profile(String name, String phone) {
        var request = new UpdateProfileRequest();
        request.setFullName(name);
        request.setPhone(phone);
        return request;
    }

    @Test
    void registerWritesUserRegisteredWithNormalizedContact() {
        long id = auth.register(registration("  New.Learner@Example.com ", "  Nguyễn Văn A ")).getId();
        var rows = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc();
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("user.registered");
        assertThat(row.getAggregateId()).isEqualTo(String.valueOf(id));
        JsonNode payload = mapper.readTree(row.getPayload());
        assertThat(payload.path("eventId").asString()).isEqualTo(row.getEventId());
        assertThat(payload.path("userId").asLong()).isEqualTo(id);
        assertThat(payload.path("email").asString()).isEqualTo("new.learner@example.com");
        assertThat(payload.path("fullName").asString()).isEqualTo("Nguyễn Văn A");
        assertThat(payload.has("password")).isFalse();
        assertThat(payload.has("passwordHash")).isFalse();
    }

    @Test
    void duplicateRegistrationWritesNothing() {
        auth.register(registration("dup@example.com", "Dup"));
        outbox.deleteAll();
        assertThatThrownBy(() -> auth.register(registration("dup@example.com", "Dup 2")))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(outbox.count()).isZero();
    }

    @Test
    void onlyNameChangesAreAnnounced() {
        long id = auth.register(registration("profile@example.com", "Tên cũ")).getId();
        outbox.deleteAll();
        auth.updateProfile(id, profile("Tên cũ", "0901234567"));
        assertThat(outbox.count()).as("đổi số điện thoại không phát sự kiện").isZero();
        auth.updateProfile(id, profile("  Tên mới ", "0901234567"));
        var rows = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc();
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getEventType()).isEqualTo("user.profile.updated");
            assertThat(mapper.readTree(row.getPayload()).path("fullName").asString()).isEqualTo("Tên mới");
        });
    }
}
