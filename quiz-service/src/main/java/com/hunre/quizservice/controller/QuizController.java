package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;
import com.hunre.quizservice.dto.UpdateQuizStatusRequest;
import com.hunre.quizservice.service.QuizService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<QuizResponse> createQuiz(
            @Valid @RequestBody CreateQuizRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return ApiResponse.ok(quizService.createQuiz(request, user.userId(), user.hasRole(Roles.ADMIN), authorization), "Tạo bài kiểm tra thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<QuizResponse> updateQuiz(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuizRequest request,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return ApiResponse.ok(quizService.updateQuiz(id, request, user.userId(), user.hasRole(Roles.ADMIN)), "Cập nhật bài kiểm tra thành công");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<QuizResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuizStatusRequest request,
            AuthenticatedUser user) {
        requireQuizManager(user);
        return switch (request.status()) {
            case PUBLISHED -> ApiResponse.ok(quizService.publishQuiz(id, user.userId(), user.hasRole(Roles.ADMIN)), "Xuất bản bài kiểm tra thành công");
            case ARCHIVED -> ApiResponse.ok(quizService.archiveQuiz(id, user.userId(), user.hasRole(Roles.ADMIN)), "Lưu trữ bài kiểm tra thành công");
            case DRAFT -> throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Chỉ hỗ trợ chuyển trạng thái sang PUBLISHED hoặc ARCHIVED");
        };
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizDetailResponse> getQuizDetail(@PathVariable Long id, AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên hoặc quản trị viên mới có quyền xem chi tiết bài kiểm tra kèm đáp án");
        }
        return ApiResponse.ok(quizService.getQuizDetail(id, user.userId(), user.hasRole(Roles.ADMIN)));
    }

    @GetMapping("/{id}/take")
    public ApiResponse<QuizDetailResponse> getQuizForStudent(@PathVariable Long id) {
        return ApiResponse.ok(quizService.getQuizForStudent(id));
    }

    @GetMapping
    public ApiResponse<List<QuizResponse>> getQuizzesByCourse(@RequestParam Long courseId, AuthenticatedUser user) {
        // Người đã mất vai trò giảng viên cũng chỉ thấy bài đã xuất bản.
        Long creatorId = user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN) ? user.userId() : null;
        return ApiResponse.ok(quizService.getQuizzesByCourse(courseId, creatorId, user.hasRole(Roles.ADMIN)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuiz(@PathVariable Long id, AuthenticatedUser user) {
        requireQuizManager(user);
        quizService.deleteQuiz(id, user.userId(), user.hasRole(Roles.ADMIN));
        return ApiResponse.message("Đã xóa bài kiểm tra");
    }

    private void requireQuizManager(AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Chỉ giảng viên hoặc quản trị viên mới có quyền quản lý bài kiểm tra");
        }
    }
}
