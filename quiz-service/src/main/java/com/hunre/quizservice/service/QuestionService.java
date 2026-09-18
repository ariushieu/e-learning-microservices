package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.UpdateQuestionRequest;

import java.util.List;

public interface QuestionService {

    QuestionResponse addQuestion(Long quizId, CreateQuestionRequest request);

    QuestionResponse updateQuestion(Long quizId, Long questionId, UpdateQuestionRequest request);

    void deleteQuestion(Long quizId, Long questionId);

    List<QuestionResponse> getQuestionsByQuiz(Long quizId);
}
