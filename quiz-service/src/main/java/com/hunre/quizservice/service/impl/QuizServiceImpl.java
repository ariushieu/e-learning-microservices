package com.hunre.quizservice.service.impl;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.service.QuizService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public QuizResponse createQuiz(CreateQuizRequest request) {
        log.info("Tạo bài kiểm tra mới cho courseId: {}, title: {}", request.getCourseId(), request.getTitle());

        Quiz quiz = Quiz.builder()
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore() != null ? request.getPassScore() : new BigDecimal("50.00"))
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3)
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .status(QuizStatus.DRAFT)
                .createdBy(request.getCreatedBy())
                .build();

        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Long id, UpdateQuizRequest request) {
        log.info("Cập nhật bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);

        if (quiz.getStatus() == QuizStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể chỉnh sửa bài kiểm tra đã lưu trữ (ARCHIVED)");
        }

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setLessonId(request.getLessonId());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        if (request.getPassScore() != null) {
            quiz.setPassScore(request.getPassScore());
        }
        if (request.getMaxAttempts() != null) {
            quiz.setMaxAttempts(request.getMaxAttempts());
        }
        quiz.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));

        Quiz updated = quizRepository.save(quiz);
        return QuizResponse.from(updated);
    }

    @Override
    @Transactional
    public QuizResponse publishQuiz(Long id) {
        log.info("Xuất bản bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);

        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bài kiểm tra phải có ít nhất 1 câu hỏi để có thể xuất bản");
        }

        quiz.setStatus(QuizStatus.PUBLISHED);
        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved);
    }

    @Override
    @Transactional
    public QuizResponse archiveQuiz(Long id) {
        log.info("Lưu trữ bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        quiz.setStatus(QuizStatus.ARCHIVED);
        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long id) {
        Quiz quiz = findQuizOrThrow(id);
        return QuizDetailResponse.from(quiz, true);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizForStudent(Long id) {
        Quiz quiz = findQuizOrThrow(id);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bài kiểm tra chưa được xuất bản nên không thể làm bài");
        }

        QuizDetailResponse detail = QuizDetailResponse.from(quiz, false);
        if (quiz.isShuffleQuestions() && detail.getQuestions() != null) {
            List<QuestionResponse> shuffled = new ArrayList<>(detail.getQuestions());
            Collections.shuffle(shuffled);
            detail.setQuestions(shuffled);
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByCourse(Long courseId) {
        return quizRepository.findByCourseId(courseId).stream()
                .map(QuizResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteQuiz(Long id) {
        log.info("Xóa bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        quizRepository.delete(quiz);
    }

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", id));
    }
}
