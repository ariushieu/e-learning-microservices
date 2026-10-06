package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.UpdateQuestionRequest;

import java.util.List;

public interface QuestionService {

    QuestionResponse addQuestion(Long quizId, CreateQuestionRequest request, Long currentUserId, boolean isAdmin);

    QuestionResponse updateQuestion(Long quizId, Long questionId, UpdateQuestionRequest request, Long currentUserId, boolean isAdmin);

    void deleteQuestion(Long quizId, Long questionId, Long currentUserId, boolean isAdmin);

    List<QuestionResponse> getQuestionsByQuiz(Long quizId, Long currentUserId, boolean isAdmin);
}
