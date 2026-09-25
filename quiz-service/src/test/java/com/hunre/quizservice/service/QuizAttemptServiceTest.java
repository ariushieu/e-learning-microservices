package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.SubmitAnswerItemRequest;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.entity.AnswerOption;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.QuestionType;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizAttempt;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.entity.OutboxEvent;
import com.hunre.quizservice.repository.OutboxEventRepository;
import com.hunre.quizservice.repository.QuizAttemptRepository;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.service.impl.QuizAttemptServiceImpl;
import com.hunre.sharedcommon.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QuizAttemptServiceImpl quizAttemptService;

    private Quiz quiz;
    private Question singleChoiceQ;
    private Question multiChoiceQ;
    private AnswerOption optA;
    private AnswerOption optB;
    private AnswerOption optC;
    private AnswerOption optD;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .id(1L)
                .courseId(100L)
                .title("Kiểm tra Java Core")
                .status(QuizStatus.PUBLISHED)
                .timeLimitMinutes(15)
                .passScore(new BigDecimal("70.00"))
                .maxAttempts(2)
                .createdBy(50L)
                .questions(new ArrayList<>())
                .build();

        // Single Choice Question: 1 point, optA (true), optB (false)
        singleChoiceQ = Question.builder()
                .id(10L)
                .quiz(quiz)
                .content("1 + 1 = ?")
                .type(QuestionType.SINGLE_CHOICE)
                .score(new BigDecimal("1.00"))
                .options(new ArrayList<>())
                .build();

        optA = AnswerOption.builder().id(101L).question(singleChoiceQ).content("2").isCorrect(true).build();
        optB = AnswerOption.builder().id(102L).question(singleChoiceQ).content("3").isCorrect(false).build();
        singleChoiceQ.getOptions().add(optA);
        singleChoiceQ.getOptions().add(optB);

        // Multiple Choice Question: 2 points, optC (true), optD (true)
        multiChoiceQ = Question.builder()
                .id(20L)
                .quiz(quiz)
                .content("Các số chẵn là:")
                .type(QuestionType.MULTIPLE_CHOICE)
                .score(new BigDecimal("2.00"))
                .options(new ArrayList<>())
                .build();

        optC = AnswerOption.builder().id(201L).question(multiChoiceQ).content("2").isCorrect(true).build();
        optD = AnswerOption.builder().id(202L).question(multiChoiceQ).content("4").isCorrect(true).build();
        multiChoiceQ.getOptions().add(optC);
        multiChoiceQ.getOptions().add(optD);

        quiz.getQuestions().add(singleChoiceQ);
        quiz.getQuestions().add(multiChoiceQ);
    }

    @Test
    @DisplayName("startAttempt: Khởi tạo lượt làm bài thành công khi quiz đã PUBLISHED")
    void startAttempt_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findFirstByQuizIdAndUserIdAndStatus(1L, 99L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository.countByQuizIdAndUserId(1L, 99L)).thenReturn(0L);

        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt a = invocation.getArgument(0);
            a.setId(500L);
            return a;
        });

        QuizAttemptResponse response = quizAttemptService.startAttempt(1L, 99L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getAttemptNo()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("startAttempt: Ném BusinessException khi đã dùng hết số lần làm bài tối đa")
    void startAttempt_maxAttemptsExceeded() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.findFirstByQuizIdAndUserIdAndStatus(1L, 99L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository.countByQuizIdAndUserId(1L, 99L)).thenReturn(2L); // max = 2

        assertThatThrownBy(() -> quizAttemptService.startAttempt(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã sử dụng hết số lần làm bài tối đa");
    }

    @Test
    @DisplayName("submitAttempt: Chấm đúng tất cả câu hỏi -> Đạt 100% và Passed = true")
    void submitAttempt_allCorrect_passed() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(500L)
                .quiz(quiz)
                .userId(99L)
                .attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(60))
                .answers(new ArrayList<>())
                .build();

        when(quizAttemptRepository.findByIdAndUserId(500L, 99L)).thenReturn(Optional.of(attempt));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder()
                .answers(List.of(
                        SubmitAnswerItemRequest.builder().questionId(10L).selectedOptionIds(Set.of(101L)).build(),
                        SubmitAnswerItemRequest.builder().questionId(20L).selectedOptionIds(Set.of(201L, 202L)).build()
                ))
                .build();

        QuizResultResponse result = quizAttemptService.submitAttempt(500L, 99L, request);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualByComparingTo("100.00");
        assertThat(result.isPassed()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        org.mockito.ArgumentCaptor<OutboxEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        OutboxEvent outbox = eventCaptor.getValue();
        assertThat(outbox.getAggregateType()).isEqualTo("QUIZ_ATTEMPT");
        assertThat(outbox.getAggregateId()).isEqualTo("500");
        assertThat(outbox.getEventType()).isEqualTo("quiz.graded");
        assertThat(outbox.getPublishedAt()).isNull();
        assertThat(objectMapper.readTree(outbox.getPayload()).get("userId").asLong()).isEqualTo(99L);
        assertThat(objectMapper.readTree(outbox.getPayload()).get("score").decimalValue())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("submitAttempt: Làm đúng 1 câu đơn (1đ/3đ = 33.33%) -> Passed = false (yêu cầu 70%)")
    void submitAttempt_partialScore_failed() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(500L)
                .quiz(quiz)
                .userId(99L)
                .attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(60))
                .answers(new ArrayList<>())
                .build();

        when(quizAttemptRepository.findByIdAndUserId(500L, 99L)).thenReturn(Optional.of(attempt));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Chọn đúng câu 1 (101L), chọn thiếu câu 2 (chỉ chọn 201L thiếu 202L)
        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder()
                .answers(List.of(
                        SubmitAnswerItemRequest.builder().questionId(10L).selectedOptionIds(Set.of(101L)).build(),
                        SubmitAnswerItemRequest.builder().questionId(20L).selectedOptionIds(Set.of(201L)).build()
                ))
                .build();

        QuizResultResponse result = quizAttemptService.submitAttempt(500L, 99L, request);

        assertThat(result).isNotNull();
        // 1/3 * 100 = 33.33%
        assertThat(result.getScore()).isEqualByComparingTo("33.33");
        assertThat(result.isPassed()).isFalse();
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
