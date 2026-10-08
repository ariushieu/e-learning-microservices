package com.hunre.quizservice.service.impl;

import com.hunre.quizservice.client.CourseOwnershipClient;
import com.hunre.quizservice.security.QuizOwnership;
import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.QuestionType;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.repository.QuizAttemptRepository;
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
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final CourseOwnershipClient courseOwnershipClient;
    private final QuizAttemptRepository attemptRepository;

    @Override
    @Transactional
    public QuizResponse createQuiz(CreateQuizRequest request, Long createdBy, boolean isAdmin, String authorization) {
        log.info("Tạo bài kiểm tra mới cho courseId: {}, title: {}", request.getCourseId(), request.getTitle());

        courseOwnershipClient.requireCourseOwner(request.getCourseId(), createdBy, isAdmin, authorization);

        Quiz quiz = Quiz.builder()
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore() != null ? request.getPassScore() : new BigDecimal("50.00"))
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3)
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .shuffleOptions(Boolean.TRUE.equals(request.getShuffleOptions()))
                .questionsPerAttempt(request.getQuestionsPerAttempt())
                .status(QuizStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved);
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(Long id, UpdateQuizRequest request, Long currentUserId, boolean isAdmin) {
        log.info("Cập nhật bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        QuizOwnership.requireOwner(quiz, currentUserId, isAdmin);

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
        quiz.setShuffleOptions(Boolean.TRUE.equals(request.getShuffleOptions()));
        quiz.setQuestionsPerAttempt(request.getQuestionsPerAttempt());

        Quiz updated = quizRepository.save(quiz);
        return QuizResponse.from(updated);
    }

    @Override
    @Transactional
    public QuizResponse publishQuiz(Long id, Long currentUserId, boolean isAdmin) {
        log.info("Xuất bản bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        QuizOwnership.requireOwner(quiz, currentUserId, isAdmin);

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
    public QuizResponse archiveQuiz(Long id, Long currentUserId, boolean isAdmin) {
        log.info("Lưu trữ bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        QuizOwnership.requireOwner(quiz, currentUserId, isAdmin);
        quiz.setStatus(QuizStatus.ARCHIVED);
        Quiz saved = quizRepository.save(quiz);
        return QuizResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long id, Long currentUserId, boolean isAdmin) {
        Quiz quiz = findQuizOrThrow(id);
        QuizOwnership.requireOwner(quiz, currentUserId, isAdmin);
        return QuizDetailResponse.from(quiz, true);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizForStudent(Long id, Long currentUserId) {
        Quiz quiz = findQuizOrThrow(id);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bài kiểm tra chưa được xuất bản nên không thể làm bài");
        }

        QuizDetailResponse detail = QuizDetailResponse.from(quiz, false);
        // A landing page has no active attempt yet. Never borrow another learner's seed.
        var attempt = attemptRepository.findFirstByQuizIdAndUserIdAndStatus(id, currentUserId, AttemptStatus.IN_PROGRESS);
        if (attempt.isEmpty()) return detail;

        var selected = attempt.get().selectedQuestions();
        detail.setQuestions(selected.stream().map(q -> QuestionResponse.from(q, false)).toList());
        detail.setTotalQuestions(selected.size());
        detail.setTotalScore(selected.stream().map(q -> q.getScore() == null ? BigDecimal.ONE : q.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Shuffle DTO copies only. Persistent question/option positions remain canonical.
        var random = new Random(attempt.get().getId());
        var shuffled = new ArrayList<>(detail.getQuestions());
        if (quiz.isShuffleQuestions()) Collections.shuffle(shuffled, random);
        if (quiz.isShuffleOptions()) {
            for (QuestionResponse question : shuffled) {
                if (question.getType() == QuestionType.TRUE_FALSE) continue;
                var options = new ArrayList<>(question.getOptions());
                Collections.shuffle(options, random);
                question.setOptions(options);
            }
        }
        detail.setQuestions(shuffled);
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByCourse(Long courseId, Long currentUserId, boolean isAdmin) {
        return (isAdmin && currentUserId != null
                ? quizRepository.findByCourseId(courseId)
                : quizRepository.findVisibleByCourseId(courseId, currentUserId, QuizStatus.PUBLISHED)).stream()
                .map(QuizResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteQuiz(Long id, Long currentUserId, boolean isAdmin) {
        log.info("Xóa bài kiểm tra id: {}", id);
        Quiz quiz = findQuizOrThrow(id);
        QuizOwnership.requireOwner(quiz, currentUserId, isAdmin);
        quizRepository.delete(quiz);
    }

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", id));
    }
}
