package com.hunre.authservice.controller;

import com.hunre.authservice.dto.LoginEventResponse;
import com.hunre.authservice.service.LoginEventService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/login-events")
@RequiredArgsConstructor
public class LoginEventController {
    private final LoginEventService events;

    @GetMapping
    public ApiResponse<PageResponse<LoginEventResponse>> list(AuthenticatedUser caller,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Phân trang không hợp lệ: page từ 0, size từ 1 đến 100, offset không vượt quá 2147483647");
        }
        return ApiResponse.ok(events.list(caller.userId(), page, size));
    }
}
