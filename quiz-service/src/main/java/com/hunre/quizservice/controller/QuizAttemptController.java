package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.service.QuizAttemptService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.startAttempt(quizId, user.userId()), "Bắt đầu làm bài kiểm tra");
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<QuizResultResponse> submitAttempt(
            @PathVariable Long attemptId,
            AuthenticatedUser user,
            @Valid @RequestBody SubmitQuizAttemptRequest request) {
        return ApiResponse.ok(quizAttemptService.submitAttempt(attemptId, user.userId(), request), "Nộp bài thành công");
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<QuizResultResponse> getAttemptResult(
            @PathVariable Long attemptId,
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.getAttemptResult(attemptId, user.userId()));
    }

    @GetMapping("/{quizId}/attempts/history")
    public ApiResponse<List<QuizAttemptResponse>> getMyAttempts(
            @PathVariable Long quizId,
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.getUserAttempts(quizId, user.userId()));
    }
}
