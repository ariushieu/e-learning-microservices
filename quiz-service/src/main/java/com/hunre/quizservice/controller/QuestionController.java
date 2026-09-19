package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.UpdateQuestionRequest;
import com.hunre.quizservice.service.QuestionService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes/{quizId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuestionResponse> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody CreateQuestionRequest request,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return ApiResponse.ok(questionService.addQuestion(quizId, request), "Thêm câu hỏi thành công");
    }

    @PutMapping("/{questionId}")
    public ApiResponse<QuestionResponse> updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return ApiResponse.ok(questionService.updateQuestion(quizId, questionId, request), "Cập nhật câu hỏi thành công");
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            AuthenticatedUser user) {
        requireQuizManager(user);
        questionService.deleteQuestion(quizId, questionId);
        return ApiResponse.message("Đã xóa câu hỏi");
    }

    @GetMapping
    public ApiResponse<List<QuestionResponse>> getQuestions(
            @PathVariable Long quizId,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return ApiResponse.ok(questionService.getQuestionsByQuiz(quizId));
    }

    private void requireQuizManager(AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Chỉ giảng viên hoặc quản trị viên mới có quyền quản lý câu hỏi và xem đáp án");
        }
    }
}
