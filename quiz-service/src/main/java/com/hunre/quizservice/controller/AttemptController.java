package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.service.QuizAttemptService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping("/{attemptId}/submit")
    public ApiResponse<QuizResultResponse> submitAttempt(
            @PathVariable Long attemptId,
            AuthenticatedUser user,
            @Valid @RequestBody SubmitQuizAttemptRequest request) {
        return ApiResponse.ok(quizAttemptService.submitAttempt(attemptId, user.userId(), request), "Nộp bài thành công");
    }

    @GetMapping("/{attemptId}")
    public ApiResponse<QuizResultResponse> getAttemptResult(
            @PathVariable Long attemptId,
            AuthenticatedUser user) {
        return ApiResponse.ok(quizAttemptService.getAttemptResult(attemptId, user.userId()));
    }
}
