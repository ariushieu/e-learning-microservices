package com.hunre.authservice.service;

import com.hunre.authservice.domain.LoginEvent;
import com.hunre.authservice.domain.User;
import com.hunre.authservice.dto.LoginEventResponse;
import com.hunre.authservice.repository.LoginEventRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoginEventService {
    private final LoginEventRepository events;

    // Joins the login transaction, whose user lock serializes attempts for this account.
    @Transactional
    public void record(User user, boolean success, String userAgent) {
        events.save(LoginEvent.builder().user(user).success(success)
                .userAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)))
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginEventResponse> list(Long userId, int page, int size) {
        var result = events.findByUserIdAndCreatedAtGreaterThanEqual(userId, cutoff(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.of(result.map(e -> new LoginEventResponse(e.isSuccess(),
                SessionService.device(e.getUserAgent()), e.getCreatedAt())).getContent(),
                page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long failedBeforeLatestSuccess(Long userId) {
        var successes = events.findTop2ByUserIdAndSuccessTrueOrderByIdDesc(userId);
        // Exclude the successful login just completed. Stable across /me and refresh calls;
        // the next successful login advances this window and clears an acknowledged warning.
        long before = successes.isEmpty() ? Long.MAX_VALUE : successes.get(0).getId();
        long after = successes.size() < 2 ? 0 : successes.get(1).getId();
        return events.countByUserIdAndSuccessFalseAndIdGreaterThanAndIdLessThanAndCreatedAtGreaterThanEqual(
                userId, after, before, cutoff());
    }

    @Scheduled(cron = "${auth.login-events.cleanup-cron:0 0 3 * * *}", zone = "UTC")
    @Transactional
    public void cleanup() {
        events.deleteExpired(cutoff());
    }

    private static Instant cutoff() {
        return Instant.now().minus(90, ChronoUnit.DAYS);
    }
}
