package com.hunre.quizservice.service.impl;

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
import com.hunre.quizservice.service.QuestionService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public QuestionResponse addQuestion(Long quizId, CreateQuestionRequest request) {
        log.info("Thêm câu hỏi mới vào bài kiểm tra id: {}", quizId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", quizId));

        validateOptions(request.getType(), request.getOptions());

        int nextPosition = request.getPosition() != null
                ? request.getPosition()
                : (quiz.getQuestions() != null ? quiz.getQuestions().size() + 1 : 1);

        Question question = Question.builder()
                .quiz(quiz)
                .content(request.getContent())
                .type(request.getType())
                .score(request.getScore() != null ? request.getScore() : new BigDecimal("1.00"))
                .position(nextPosition)
                .explanation(request.getExplanation())
                .build();

        for (int i = 0; i < request.getOptions().size(); i++) {
            AnswerOptionRequest optReq = request.getOptions().get(i);
            AnswerOption option = AnswerOption.builder()
                    .content(optReq.getContent())
                    .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                    .position(optReq.getPosition() != null ? optReq.getPosition() : (i + 1))
                    .build();
            question.addOption(option);
        }

        Question saved = questionRepository.save(question);
        return QuestionResponse.from(saved, true);
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(Long quizId, Long questionId, UpdateQuestionRequest request) {
        log.info("Cập nhật câu hỏi id: {} của bài kiểm tra id: {}", questionId, quizId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("câu hỏi", "id", questionId));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Câu hỏi không thuộc bài kiểm tra này");
        }

        validateOptions(request.getType(), request.getOptions());

        question.setContent(request.getContent());
        question.setType(request.getType());
        if (request.getScore() != null) {
            question.setScore(request.getScore());
        }
        if (request.getPosition() != null) {
            question.setPosition(request.getPosition());
        }
        question.setExplanation(request.getExplanation());

        // Cập nhật lại danh sách options
        question.getOptions().clear();
        for (int i = 0; i < request.getOptions().size(); i++) {
            AnswerOptionRequest optReq = request.getOptions().get(i);
            AnswerOption option = AnswerOption.builder()
                    .content(optReq.getContent())
                    .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                    .position(optReq.getPosition() != null ? optReq.getPosition() : (i + 1))
                    .build();
            question.addOption(option);
        }

        Question updated = questionRepository.save(question);
        return QuestionResponse.from(updated, true);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long quizId, Long questionId) {
        log.info("Xóa câu hỏi id: {} của bài kiểm tra id: {}", questionId, quizId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("câu hỏi", "id", questionId));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Câu hỏi không thuộc bài kiểm tra này");
        }

        questionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByQuiz(Long quizId) {
        return questionRepository.findByQuizIdOrderByPositionAsc(quizId).stream()
                .map(q -> QuestionResponse.from(q, true))
                .toList();
    }

    private void validateOptions(QuestionType type, List<AnswerOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Câu hỏi phải có ít nhất 2 phương án trả lời");
        }

        long correctCount = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).count();

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE) {
            if (correctCount != 1) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Câu hỏi đơn lựa chọn / Đúng-Sai phải có đúng 1 đáp án chính xác");
            }
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            if (correctCount < 1) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Câu hỏi nhiều lựa chọn phải có ít nhất 1 đáp án chính xác");
            }
        }
    }
}
