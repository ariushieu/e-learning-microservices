package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;

import java.util.List;

public interface QuizAttemptService {

    QuizAttemptResponse startAttempt(Long quizId, Long userId);

    QuizResultResponse submitAttempt(Long attemptId, Long userId, SubmitQuizAttemptRequest request);

    QuizResultResponse getAttemptResult(Long attemptId, Long userId);

    List<QuizAttemptResponse> getUserAttempts(Long quizId, Long userId);
}
