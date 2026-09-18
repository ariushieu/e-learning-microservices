package com.hunre.quizservice.service.impl;

import com.hunre.quizservice.dto.AnswerOptionResponse;
import com.hunre.quizservice.dto.QuestionResultResponse;
import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitAnswerItemRequest;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.entity.AnswerOption;
import com.hunre.quizservice.entity.AttemptAnswer;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizAttempt;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.event.QuizEventPublisher;
import com.hunre.quizservice.repository.QuizAttemptRepository;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.service.QuizAttemptService;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizEventPublisher quizEventPublisher;

    @Override
    @Transactional
    public QuizAttemptResponse startAttempt(Long quizId, Long userId) {
        log.info("Bắt đầu lượt làm bài quizId: {} cho userId: {}", quizId, userId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", quizId));

        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bài kiểm tra chưa được xuất bản nên không thể làm bài");
        }

        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bài kiểm tra hiện chưa có câu hỏi");
        }

        // Kiểm tra xem có lượt nào đang làm dở không
        Optional<QuizAttempt> existingAttempt = quizAttemptRepository
                .findFirstByQuizIdAndUserIdAndStatus(quizId, userId, AttemptStatus.IN_PROGRESS);

        if (existingAttempt.isPresent()) {
            QuizAttempt ongoing = existingAttempt.get();
            // Kiểm tra xem đã hết giờ chưa
            if (isAttemptTimeExpired(ongoing)) {
                markAttemptExpired(ongoing);
            } else {
                // Vẫn còn giờ, trả về lượt đang làm để học viên làm tiếp
                return QuizAttemptResponse.from(ongoing);
            }
        }

        // Kiểm tra số lần làm bài tối đa
        long attemptsCount = quizAttemptRepository.countByQuizIdAndUserId(quizId, userId);
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && attemptsCount >= quiz.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Bạn đã sử dụng hết số lần làm bài tối đa (" + quiz.getMaxAttempts() + " lần)");
        }

        QuizAttempt newAttempt = QuizAttempt.builder()
                .quiz(quiz)
                .userId(userId)
                .attemptNo((int) attemptsCount + 1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();

        QuizAttempt saved = quizAttemptRepository.save(newAttempt);
        return QuizAttemptResponse.from(saved);
    }

    @Override
    @Transactional
    public QuizResultResponse submitAttempt(Long attemptId, Long userId, SubmitQuizAttemptRequest request) {
        log.info("Nộp bài kiểm tra attemptId: {} cho userId: {}", attemptId, userId);
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt làm bài", "id", attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Lượt làm bài đã được nộp hoặc đã hết hạn trước đó");
        }

        Quiz quiz = attempt.getQuiz();

        // Kiểm tra thời gian làm bài (cho phép dư 30 giây bù độ trễ mạng)
        if (isAttemptTimeExpired(attempt)) {
            markAttemptExpired(attempt);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Thời gian làm bài đã kết thúc, bài thi không được chấm");
        }

        // Map câu trả lời của học viên: questionId -> Set<optionId>
        Map<Long, Set<Long>> answersMap = request.getAnswers().stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(
                        SubmitAnswerItemRequest::getQuestionId,
                        a -> a.getSelectedOptionIds() != null ? a.getSelectedOptionIds() : Set.of(),
                        (existing, replacement) -> replacement
                ));

        BigDecimal totalEarnedScore = BigDecimal.ZERO;
        BigDecimal totalMaxScore = BigDecimal.ZERO;
        List<QuestionResultResponse> questionResults = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            BigDecimal questionScore = question.getScore() != null ? question.getScore() : BigDecimal.ONE;
            totalMaxScore = totalMaxScore.add(questionScore);

            Set<Long> selectedOptionIds = answersMap.getOrDefault(question.getId(), Set.of());

            // Tìm các đáp án đúng của câu hỏi
            Set<Long> correctOptionIds = question.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());

            // Chấm điểm câu hỏi
            boolean isQuestionCorrect = !correctOptionIds.isEmpty() && correctOptionIds.equals(selectedOptionIds);
            BigDecimal earnedScore = isQuestionCorrect ? questionScore : BigDecimal.ZERO;
            totalEarnedScore = totalEarnedScore.add(earnedScore);

            // Tìm danh sách AnswerOption tương ứng với selectedOptionIds
            Map<Long, AnswerOption> optionEntityMap = question.getOptions().stream()
                    .collect(Collectors.toMap(AnswerOption::getId, o -> o));

            Set<AnswerOption> selectedOptionEntities = selectedOptionIds.stream()
                    .map(optionEntityMap::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            AttemptAnswer attemptAnswer = AttemptAnswer.builder()
                    .question(question)
                    .earnedScore(earnedScore)
                    .isCorrect(isQuestionCorrect)
                    .selectedOptions(selectedOptionEntities)
                    .build();

            attempt.addAnswer(attemptAnswer);

            // DTO chi tiết kết quả câu hỏi
            questionResults.add(QuestionResultResponse.builder()
                    .questionId(question.getId())
                    .content(question.getContent())
                    .type(question.getType())
                    .questionScore(questionScore)
                    .earnedScore(earnedScore)
                    .isCorrect(isQuestionCorrect)
                    .explanation(question.getExplanation())
                    .selectedOptionIds(selectedOptionIds)
                    .correctOptionIds(correctOptionIds)
                    .options(question.getOptions().stream()
                            .map(opt -> AnswerOptionResponse.from(opt, true))
                            .toList())
                    .build());
        }

        // Tính điểm tổng theo phần trăm (0 - 100)
        BigDecimal finalScorePercent;
        if (totalMaxScore.compareTo(BigDecimal.ZERO) > 0) {
            finalScorePercent = totalEarnedScore
                    .multiply(new BigDecimal(100))
                    .divide(totalMaxScore, 2, RoundingMode.HALF_UP);
        } else {
            finalScorePercent = BigDecimal.ZERO;
        }

        boolean passed = finalScorePercent.compareTo(quiz.getPassScore()) >= 0;

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setScore(finalScorePercent);
        attempt.setPassed(passed);
        attempt.setSubmittedAt(Instant.now());

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // Bắn sự kiện sang Kafka
        QuizGradedEvent event = QuizGradedEvent.of(
                savedAttempt.getId(),
                quiz.getId(),
                quiz.getCourseId(),
                userId,
                quiz.getTitle(),
                finalScorePercent,
                passed
        );
        quizEventPublisher.publishQuizGraded(event);

        return QuizResultResponse.builder()
                .attemptId(savedAttempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .userId(userId)
                .attemptNo(savedAttempt.getAttemptNo())
                .score(finalScorePercent)
                .passScore(quiz.getPassScore())
                .passed(passed)
                .startedAt(savedAttempt.getStartedAt())
                .submittedAt(savedAttempt.getSubmittedAt())
                .questionResults(questionResults)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultResponse getAttemptResult(Long attemptId, Long userId) {
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt làm bài", "id", attemptId));

        Quiz quiz = attempt.getQuiz();

        Map<Long, AttemptAnswer> answerMap = attempt.getAnswers().stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (oldV, newV) -> newV));

        List<QuestionResultResponse> questionResults = new ArrayList<>();
        for (Question question : quiz.getQuestions()) {
            AttemptAnswer ans = answerMap.get(question.getId());

            Set<Long> selectedOptionIds = ans != null && ans.getSelectedOptions() != null
                    ? ans.getSelectedOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet())
                    : Set.of();

            Set<Long> correctOptionIds = question.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());

            questionResults.add(QuestionResultResponse.builder()
                    .questionId(question.getId())
                    .content(question.getContent())
                    .type(question.getType())
                    .questionScore(question.getScore())
                    .earnedScore(ans != null ? ans.getEarnedScore() : BigDecimal.ZERO)
                    .isCorrect(ans != null && ans.isCorrect())
                    .explanation(question.getExplanation())
                    .selectedOptionIds(selectedOptionIds)
                    .correctOptionIds(correctOptionIds)
                    .options(question.getOptions().stream()
                            .map(opt -> AnswerOptionResponse.from(opt, true))
                            .toList())
                    .build());
        }

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .userId(userId)
                .attemptNo(attempt.getAttemptNo())
                .score(attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO)
                .passScore(quiz.getPassScore())
                .passed(Boolean.TRUE.equals(attempt.getPassed()))
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .questionResults(questionResults)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getUserAttempts(Long quizId, Long userId) {
        return quizAttemptRepository.findByQuizIdAndUserIdOrderByAttemptNoDesc(quizId, userId).stream()
                .map(QuizAttemptResponse::from)
                .toList();
    }

    private boolean isAttemptTimeExpired(QuizAttempt attempt) {
        Integer limitMinutes = attempt.getQuiz().getTimeLimitMinutes();
        if (limitMinutes == null || limitMinutes <= 0) {
            return false;
        }
        // Thêm 30 giây ân hạn độ trễ mạng
        long allowedSeconds = (limitMinutes * 60L) + 30L;
        long elapsedSeconds = Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();
        return elapsedSeconds > allowedSeconds;
    }

    private void markAttemptExpired(QuizAttempt attempt) {
        attempt.setStatus(AttemptStatus.EXPIRED);
        attempt.setScore(BigDecimal.ZERO);
        attempt.setPassed(false);
        attempt.setSubmittedAt(Instant.now());
        quizAttemptRepository.save(attempt);
    }
}
