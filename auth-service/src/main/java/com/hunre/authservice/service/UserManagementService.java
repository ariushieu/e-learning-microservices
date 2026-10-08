package com.hunre.authservice.service;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.User;
import com.hunre.authservice.domain.UserStatus;
import com.hunre.authservice.dto.UserResponse;
import com.hunre.authservice.dto.UserStatsResponse;
import com.hunre.authservice.repository.RefreshTokenRepository;
import com.hunre.authservice.repository.UserRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "email", "fullName");

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public UserStatsResponse statistics() {
        Instant now = Instant.now();
        var byRole = new EnumMap<RoleCode, Long>(RoleCode.class);
        var byStatus = new EnumMap<UserStatus, Long>(UserStatus.class);
        for (RoleCode role : RoleCode.values()) byRole.put(role, 0L);
        for (UserStatus status : UserStatus.values()) byStatus.put(status, 0L);
        users.countByRole().forEach(row -> byRole.put(row.getRole(), row.getTotal()));
        users.countByStatus().forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));
        // Status partitions users; roles overlap for accounts with multiple roles.
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new UserStatsResponse(total, byRole, byStatus,
                users.countCreatedBetween(now.minus(7, ChronoUnit.DAYS), now));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String keyword, RoleCode role, UserStatus status, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ sắp xếp theo createdAt, email hoặc fullName");
            }
        }
        String text = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Specification<User> filter = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!text.isEmpty()) {
                predicates.add(cb.or(cb.like(cb.lower(root.get("email")), pattern, '!'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '!')));
            }
            if (role != null) predicates.add(cb.equal(root.join("roles").get("code"), role));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        // Stable pages when several accounts have the same name or creation timestamp.
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        var page = users.findAll(filter, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                sort.and(Sort.by("id"))));
        return PageResponse.of(page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status, Long callerId) {
        // Same lock order as login, refresh, password changes and role changes: user first.
        User user = users.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("người dùng", "id", id));
        if (status == UserStatus.LOCKED && (id.equals(callerId)
                || user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.ROLE_ADMIN))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể khóa chính mình hoặc tài khoản quản trị viên");
        }
        user.setStatus(status);
        if (status == UserStatus.LOCKED) refreshTokens.revokeAllUserTokens(id, Instant.now());
        return UserResponse.from(users.save(user));
    }
}
