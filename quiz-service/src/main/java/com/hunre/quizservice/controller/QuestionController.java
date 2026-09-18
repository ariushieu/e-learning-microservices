package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.UpdateQuestionRequest;
import com.hunre.quizservice.service.QuestionService;
import com.hunre.sharedcommon.dto.ApiResponse;
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
            @Valid @RequestBody CreateQuestionRequest request) {
        return ApiResponse.ok(questionService.addQuestion(quizId, request), "Thêm câu hỏi thành công");
    }

    @PutMapping("/{questionId}")
    public ApiResponse<QuestionResponse> updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return ApiResponse.ok(questionService.updateQuestion(quizId, questionId, request), "Cập nhật câu hỏi thành công");
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId) {
        questionService.deleteQuestion(quizId, questionId);
        return ApiResponse.message("Đã xóa câu hỏi");
    }

    @GetMapping
    public ApiResponse<List<QuestionResponse>> getQuestions(@PathVariable Long quizId) {
        return ApiResponse.ok(questionService.getQuestionsByQuiz(quizId));
    }
}
