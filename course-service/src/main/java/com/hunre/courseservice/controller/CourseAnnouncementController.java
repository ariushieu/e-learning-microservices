package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.PostAnnouncementRequest;
import com.hunre.courseservice.dto.response.CourseAnnouncementResponse;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.courseservice.service.CourseAnnouncementService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/announcements")
@RequiredArgsConstructor
public class CourseAnnouncementController {
    private final CourseAnnouncementService service;
    private final CurrentUserProvider currentUser;

    // GET khóa học công khai ở gateway, nên tự đọc danh tính; service trả 401 nếu không có.
    @GetMapping
    public ApiResponse<PageResponse<CourseAnnouncementResponse>> list(@PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(courseId, page, size, currentUser.getCurrentUser().orElse(null)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseAnnouncementResponse> post(@PathVariable Long courseId,
            @Valid @RequestBody PostAnnouncementRequest request, AuthenticatedUser user) {
        var saved = service.post(courseId, request, user);
        return ApiResponse.ok(saved, saved.recipientCount() == 0
                ? "Đã đăng thông báo. Khóa học chưa có học viên nên chưa ai nhận được"
                : "Đã gửi thông báo tới " + saved.recipientCount() + " học viên");
    }

    @DeleteMapping("/{announcementId}")
    public ApiResponse<Void> delete(@PathVariable Long courseId, @PathVariable Long announcementId, AuthenticatedUser user) {
        service.delete(courseId, announcementId, user);
        return ApiResponse.message("Đã gỡ thông báo khỏi trang khóa học");
    }
}
