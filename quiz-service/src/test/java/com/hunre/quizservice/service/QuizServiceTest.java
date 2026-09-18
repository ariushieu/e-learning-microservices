package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.dto.UpdateQuizRequest;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.service.impl.QuizServiceImpl;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
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
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .id(1L)
                .courseId(10L)
                .title("Bài kiểm tra 1")
                .description("Mô tả bài kiểm tra")
                .passScore(new BigDecimal("50.00"))
                .maxAttempts(3)
                .status(QuizStatus.DRAFT)
                .createdBy(100L)
                .questions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("createQuiz: Tạo bài kiểm tra với trạng thái DRAFT mặc định")
    void createQuiz_success() {
        CreateQuizRequest request = CreateQuizRequest.builder()
                .courseId(10L)
                .title("Bài kiểm tra 1")
                .createdBy(100L)
                .build();

        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            q.setId(1L);
            return q;
        });

        QuizResponse response = quizService.createQuiz(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(QuizStatus.DRAFT);
    }

    @Test
    @DisplayName("updateQuiz: Ném BusinessException nếu bài kiểm tra đã ARCHIVED")
    void updateQuiz_archived_throwsException() {
        quiz.setStatus(QuizStatus.ARCHIVED);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        UpdateQuizRequest request = UpdateQuizRequest.builder()
                .title("Tiêu đề mới")
                .build();

        assertThatThrownBy(() -> quizService.updateQuiz(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chỉnh sửa bài kiểm tra đã lưu trữ");
    }

    @Test
    @DisplayName("publishQuiz: Ném BusinessException nếu bài kiểm tra chưa có câu hỏi nào")
    void publishQuiz_emptyQuestions_throwsException() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> quizService.publishQuiz(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phải có ít nhất 1 câu hỏi để có thể xuất bản");
    }

    @Test
    @DisplayName("publishQuiz: Xuất bản thành công khi đã có ít nhất 1 câu hỏi")
    void publishQuiz_success() {
        quiz.getQuestions().add(Question.builder().id(10L).content("Câu 1").build());
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResponse response = quizService.publishQuiz(1L);

        assertThat(response.getStatus()).isEqualTo(QuizStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getQuizForStudent: Ném BusinessException nếu quiz chưa PUBLISHED")
    void getQuizForStudent_notPublished_throwsException() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz)); // DRAFT

        assertThatThrownBy(() -> quizService.getQuizForStudent(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa được xuất bản");
    }

    @Test
    @DisplayName("deleteQuiz: Xóa quiz thành công")
    void deleteQuiz_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));

        quizService.deleteQuiz(1L);

        verify(quizRepository).delete(quiz);
    }
}
