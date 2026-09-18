package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.AnswerOptionRequest;
import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.dto.UpdateQuestionRequest;
import com.hunre.quizservice.entity.AnswerOption;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.QuestionType;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.repository.QuestionRepository;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.service.impl.QuestionServiceImpl;
import com.hunre.sharedcommon.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .id(1L)
                .title("Quiz 1")
                .questions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("addQuestion: Thêm câu hỏi thành công khi options hợp lệ")
    void addQuestion_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId(10L);
            return q;
        });

        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .content("Thủ đô của Việt Nam là gì?")
                .type(QuestionType.SINGLE_CHOICE)
                .score(new BigDecimal("1.00"))
                .options(List.of(
                        AnswerOptionRequest.builder().content("Hà Nội").isCorrect(true).build(),
                        AnswerOptionRequest.builder().content("TP.HCM").isCorrect(false).build()
                ))
                .build();

        QuestionResponse response = questionService.addQuestion(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOptions()).hasSize(2);
    }

    @Test
    @DisplayName("addQuestion: Ném BusinessException khi SINGLE_CHOICE có 2 đáp án đúng")
    void addQuestion_singleChoice_twoCorrect_throwsException() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .content("Câu hỏi lỗi")
                .type(QuestionType.SINGLE_CHOICE)
                .options(List.of(
                        AnswerOptionRequest.builder().content("A").isCorrect(true).build(),
                        AnswerOptionRequest.builder().content("B").isCorrect(true).build()
                ))
                .build();

        assertThatThrownBy(() -> questionService.addQuestion(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phải có đúng 1 đáp án chính xác");
    }

    @Test
    @DisplayName("deleteQuestion: Xóa câu hỏi thành công")
    void deleteQuestion_success() {
        Question question = Question.builder().id(10L).quiz(quiz).build();
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        questionService.deleteQuestion(1L, 10L);

        verify(questionRepository).delete(question);
    }
}
