package com.hunre.authservice.dto;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.UserStatus;

import java.util.Map;

public record UserStatsResponse(long total, Map<RoleCode, Long> byRole,
                                Map<UserStatus, Long> byStatus, long newLast7Days) {
}
