package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.service.QuizAttemptService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping("/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizAttemptResponse> startAttempt(
            @PathVariable Long quizId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.startAttempt(
                quizId, user.userId(), user.hasRole(Roles.ADMIN), authorization, user.fullName()), "Bắt đầu làm bài kiểm tra");
    }

    @GetMapping("/{quizId}/attempts")
    public ApiResponse<List<QuizAttemptResponse>> getMyAttempts(
            @PathVariable Long quizId,
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.getUserAttempts(quizId, user.userId()));
    }
}
