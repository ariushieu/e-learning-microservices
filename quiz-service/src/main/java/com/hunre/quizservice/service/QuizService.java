package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;

import java.util.List;

public interface QuizService {

    QuizResponse createQuiz(CreateQuizRequest request, Long createdBy, boolean isAdmin, String authorization);

    QuizResponse updateQuiz(Long id, UpdateQuizRequest request, Long currentUserId, boolean isAdmin);

    QuizResponse publishQuiz(Long id, Long currentUserId, boolean isAdmin);

    QuizResponse archiveQuiz(Long id, Long currentUserId, boolean isAdmin);

    QuizDetailResponse getQuizDetail(Long id, Long currentUserId, boolean isAdmin);

    QuizDetailResponse getQuizForStudent(Long id, Long currentUserId);

    List<QuizResponse> getQuizzesByCourse(Long courseId, Long currentUserId, boolean isAdmin);

    void deleteQuiz(Long id, Long currentUserId, boolean isAdmin);
}
