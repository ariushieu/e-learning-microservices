package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.QuizResultsResponse;
import com.hunre.quizservice.service.QuizResultsService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizResultsController {
    private final QuizResultsService results;

    @GetMapping("/{quizId}/results")
    public ApiResponse<QuizResultsResponse> getResults(@PathVariable Long quizId, AuthenticatedUser user,
            @PageableDefault(size = 20, sort = "lastSubmittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(results.getResults(quizId, user, pageable));
    }
}
