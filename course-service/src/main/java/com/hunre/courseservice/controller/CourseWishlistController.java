package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.service.CourseWishlistService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class CourseWishlistController {
    private final CourseWishlistService service;

    @GetMapping
    public ApiResponse<List<CourseSummaryResponse>> list(AuthenticatedUser user) {
        return ApiResponse.ok(service.list(user));
    }

    /** Chỉ id, để trang danh mục tô trái tim mà không phải tải lại cả thông tin khóa. */
    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(AuthenticatedUser user) {
        return ApiResponse.ok(service.ids(user));
    }

    @PutMapping("/{courseId}")
    public ApiResponse<Void> add(@PathVariable Long courseId, AuthenticatedUser user) {
        service.add(courseId, user);
        return ApiResponse.message("Đã lưu vào danh sách yêu thích");
    }

    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> remove(@PathVariable Long courseId, AuthenticatedUser user) {
        service.remove(courseId, user);
        return ApiResponse.message("Đã bỏ khỏi danh sách yêu thích");
    }
}
