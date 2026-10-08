package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.response.InstructorProfileResponse;
import com.hunre.courseservice.service.InstructorProfileService;
import com.hunre.sharedcommon.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructors")
public class InstructorProfileController {
    private final InstructorProfileService profiles;

    @GetMapping("/{id}")
    public ApiResponse<InstructorProfileResponse> getProfile(@PathVariable Long id) {
        return ApiResponse.ok(profiles.getProfile(id));
    }
}
