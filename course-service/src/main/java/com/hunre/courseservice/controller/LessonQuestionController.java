package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.PostLessonTextRequest;
import com.hunre.courseservice.dto.response.LessonQuestionResponse;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.courseservice.service.LessonQuestionService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LessonQuestionController {
    private final LessonQuestionService service;
    private final CurrentUserProvider currentUser;

    // GET /api/lessons/** công khai ở gateway, nên tự đọc danh tính; service trả 401 nếu không có.
    @GetMapping("/api/lessons/{lessonId}/questions")
    public ApiResponse<PageResponse<LessonQuestionResponse>> list(@PathVariable Long lessonId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(lessonId, page, size, currentUser.getCurrentUser().orElse(null)));
    }

    @PostMapping("/api/lessons/{lessonId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonQuestionResponse> ask(@PathVariable Long lessonId,
            @Valid @RequestBody PostLessonTextRequest request, AuthenticatedUser user) {
        return ApiResponse.ok(service.ask(lessonId, request.content(), user), "Đã gửi câu hỏi");
    }

    @PostMapping("/api/lessons/{lessonId}/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonQuestionResponse> answer(@PathVariable Long lessonId, @PathVariable Long questionId,
            @Valid @RequestBody PostLessonTextRequest request, AuthenticatedUser user) {
        return ApiResponse.ok(service.answer(lessonId, questionId, request.content(), user), "Đã gửi câu trả lời");
    }

    @DeleteMapping("/api/lessons/{lessonId}/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long lessonId, @PathVariable Long questionId,
                                            AuthenticatedUser user) {
        service.deleteQuestion(lessonId, questionId, user);
        return ApiResponse.message("Đã xóa câu hỏi");
    }

    @DeleteMapping("/api/lessons/{lessonId}/questions/{questionId}/answers/{answerId}")
    public ApiResponse<Void> deleteAnswer(@PathVariable Long lessonId, @PathVariable Long questionId,
                                          @PathVariable Long answerId, AuthenticatedUser user) {
        service.deleteAnswer(lessonId, questionId, answerId, user);
        return ApiResponse.message("Đã xóa câu trả lời");
    }

    @GetMapping("/api/instructor/questions")
    public ApiResponse<PageResponse<LessonQuestionResponse>> inbox(AuthenticatedUser user,
            @RequestParam(required = false) Boolean answered, @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.inbox(answered, courseId, page, size, user));
    }

    @GetMapping("/api/instructor/questions/unanswered-count")
    public ApiResponse<Map<String, Long>> unansweredCount(AuthenticatedUser user) {
        return ApiResponse.ok(Map.of("count", service.countUnanswered(user)));
    }
}
