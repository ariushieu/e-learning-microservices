package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;

import java.util.List;

public interface QuizService {

    QuizResponse createQuiz(CreateQuizRequest request);

    QuizResponse updateQuiz(Long id, UpdateQuizRequest request);

    QuizResponse publishQuiz(Long id);

    QuizResponse archiveQuiz(Long id);

    QuizDetailResponse getQuizDetail(Long id);

    QuizDetailResponse getQuizForStudent(Long id);

    List<QuizResponse> getQuizzesByCourse(Long courseId);

    void deleteQuiz(Long id);
}
