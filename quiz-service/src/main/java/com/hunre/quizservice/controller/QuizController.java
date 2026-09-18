package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;
import com.hunre.quizservice.service.QuizService;
import com.hunre.sharedcommon.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizResponse> createQuiz(@Valid @RequestBody CreateQuizRequest request) {
        return ApiResponse.ok(quizService.createQuiz(request), "Tạo bài kiểm tra thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<QuizResponse> updateQuiz(@PathVariable Long id, @Valid @RequestBody UpdateQuizRequest request) {
        return ApiResponse.ok(quizService.updateQuiz(id, request), "Cập nhật bài kiểm tra thành công");
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<QuizResponse> publishQuiz(@PathVariable Long id) {
        return ApiResponse.ok(quizService.publishQuiz(id), "Xuất bản bài kiểm tra thành công");
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<QuizResponse> archiveQuiz(@PathVariable Long id) {
        return ApiResponse.ok(quizService.archiveQuiz(id), "Lưu trữ bài kiểm tra thành công");
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizDetailResponse> getQuizDetail(@PathVariable Long id) {
        return ApiResponse.ok(quizService.getQuizDetail(id));
    }

    @GetMapping("/{id}/take")
    public ApiResponse<QuizDetailResponse> getQuizForStudent(@PathVariable Long id) {
        return ApiResponse.ok(quizService.getQuizForStudent(id));
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<QuizResponse>> getQuizzesByCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(quizService.getQuizzesByCourse(courseId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ApiResponse.message("Đã xóa bài kiểm tra");
    }
}
