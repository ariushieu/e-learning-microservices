package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.leaderboard.LeaderboardPeriod;
import com.hunre.enrollmentservice.leaderboard.LeaderboardService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bảng xếp hạng học viên: 10 điểm mỗi bài học, 100 điểm mỗi khóa hoàn thành. */
@RestController
@Validated
@RequiredArgsConstructor
public class LeaderboardController {
    private final LeaderboardService leaderboard;

    @GetMapping("/api/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardService.Result>> get(
            AuthenticatedUser user,
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để xem bảng xếp hạng");
        }
        LeaderboardPeriod parsed;
        try {
            parsed = LeaderboardPeriod.parse(period);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "period chỉ nhận all hoặc week");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(leaderboard.get(parsed, limit, user.userId())));
    }
}
