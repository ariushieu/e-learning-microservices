package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateSectionRequest;
import com.hunre.courseservice.dto.response.SectionResponse;
import com.hunre.courseservice.service.CurriculumService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SectionController {

    private final CurriculumService curriculumService;

    @GetMapping("/api/courses/{courseId}/curriculum")
    public ApiResponse<List<SectionResponse>> getCurriculumByCourseId(@PathVariable Long courseId) {
        return ApiResponse.ok(curriculumService.getCurriculumByCourseId(courseId));
    }

    @PostMapping("/api/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SectionResponse> createSection(@Valid @RequestBody CreateSectionRequest request) {
        return ApiResponse.ok(curriculumService.createSection(request), "Tạo chương học thành công");
    }

    @PutMapping("/api/sections/{id}")
    public ApiResponse<SectionResponse> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSectionRequest request) {
        return ApiResponse.ok(curriculumService.updateSection(id, request), "Cập nhật chương học thành công");
    }

    @DeleteMapping("/api/sections/{id}")
    public ApiResponse<Void> deleteSection(@PathVariable Long id) {
        curriculumService.deleteSection(id);
        return ApiResponse.message("Đã xóa chương học");
    }
}
