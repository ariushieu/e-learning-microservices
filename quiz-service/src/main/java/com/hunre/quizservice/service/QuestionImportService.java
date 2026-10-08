package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.AnswerOptionRequest;
import com.hunre.quizservice.dto.CreateQuestionRequest;
import com.hunre.quizservice.entity.QuestionType;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.quizservice.security.QuizOwnership;
import com.hunre.quizservice.service.QuestionImportException.RowError;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionImportService {
    public static final int MAX_BYTES = 1024 * 1024;
    private final QuizRepository quizzes;
    private final QuestionService questions;

    @Transactional
    public int importFile(Long quizId, MultipartFile file, AuthenticatedUser user) {
        Quiz quiz = requireAccess(quizId, user);
        if (file == null || file.isEmpty()) throw new QuestionImportException(1, "Chọn file CSV có câu hỏi.");
        if (file.getSize() > MAX_BYTES) throw new QuestionImportException(1, "File tối đa 1 MB (1048576 byte).");
        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException ex) {
            throw new QuestionImportException(1, "Không đọc được file CSV.");
        }
        if (bytes.length > MAX_BYTES) throw new QuestionImportException(1, "File tối đa 1 MB (1048576 byte).");
        var requests = parse(decode(bytes));
        int position = quiz.getQuestions().stream().mapToInt(q -> q.getPosition()).max().orElse(0);
        // Validate the whole file before any write; nested addQuestion calls join this transaction.
        for (var request : requests) {
            request.setPosition(++position);
            questions.addQuestion(quizId, request, user.userId(), user.hasRole(Roles.ADMIN));
        }
        return requests.size();
    }

    @Transactional(readOnly = true)
    public byte[] template(Long quizId, AuthenticatedUser user) throws IOException {
        requireAccess(quizId, user);
        try (var stream = new ClassPathResource("csv/questions-template.csv").getInputStream()) {
            return stream.readAllBytes();
        }
    }

    private Quiz requireAccess(Long quizId, AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên hoặc quản trị viên được nhập câu hỏi");
        }
        var quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", quizId));
        QuizOwnership.requireOwner(quiz, user.userId(), user.hasRole(Roles.ADMIN));
        return quiz;
    }

    private static String decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException ex) {
            throw new QuestionImportException(1, "File phải là CSV UTF-8; trong Excel chọn CSV UTF-8 khi lưu.");
        }
    }

    private static List<CreateQuestionRequest> parse(String text) {
        char delimiter = text.startsWith("type;") || text.startsWith("\"type\";") ? ';' : ',';
        var format = CSVFormat.RFC4180.builder().setDelimiter(delimiter).get();
        var requests = new ArrayList<CreateQuestionRequest>();
        var errors = new ArrayList<RowError>();
        long line = 1;
        try (var parser = format.parse(new StringReader(text))) {
            var rows = parser.iterator();
            if (!rows.hasNext()) throw new QuestionImportException(1, "File thiếu dòng tiêu đề.");
            var header = rows.next();
            var fixed = List.of("type", "content", "score", "explanation");
            if (header.size() < 6 || header.size() > 24) {
                throw new QuestionImportException(1, "Tiêu đề cần type,content,score,explanation và option1..optionN (2–20 đáp án).");
            }
            for (int i = 0; i < header.size(); i++) {
                String expected = i < 4 ? fixed.get(i) : "option" + (i - 3);
                if (!header.get(i).equals(expected)) throw new QuestionImportException(1, "Cột " + (i + 1) + " phải là " + expected);
            }
            int count = 0;
            while (true) {
                line = parser.getCurrentLineNumber() + 1;
                if (!rows.hasNext()) break;
                CSVRecord row = rows.next();
                if (++count > 200) throw new QuestionImportException(line, "Tối đa 200 dòng câu hỏi.");
                if (row.size() != header.size()) {
                    errors.add(new RowError(line, "Số cột không khớp tiêu đề; dùng file mẫu và giữ các cột trống cuối dòng."));
                    continue;
                }
                validateRow(row, line, requests, errors);
            }
        } catch (IOException | UncheckedIOException ex) {
            errors.add(new RowError(line, "CSV sai cú pháp: kiểm tra dấu ngoặc kép và dấu phân cách."));
        }
        if (!errors.isEmpty()) throw new QuestionImportException(errors);
        if (requests.isEmpty()) throw new QuestionImportException(2, "File chưa có câu hỏi.");
        return requests;
    }

    private static void validateRow(CSVRecord row, long line, List<CreateQuestionRequest> requests, List<RowError> errors) {
        int before = errors.size();
        QuestionType type = null;
        try { type = QuestionType.valueOf(row.get(0).trim()); }
        catch (IllegalArgumentException ex) { errors.add(new RowError(line, "Loại câu hỏi phải là SINGLE_CHOICE, MULTIPLE_CHOICE hoặc TRUE_FALSE.")); }
        String content = row.get(1), explanation = row.get(3);
        if (content.isBlank() || content.length() > 10000) errors.add(new RowError(line, "Nội dung cần 1–10000 ký tự."));
        if (explanation.length() > 10000) errors.add(new RowError(line, "Giải thích tối đa 10000 ký tự."));
        BigDecimal score = null;
        String scoreText = row.get(2).trim();
        if (!scoreText.matches("[0-9]{1,3}(\\.[0-9]{1,2})?")) {
            errors.add(new RowError(line, "Điểm từ 0.01 đến 999.99, tối đa 2 chữ số thập phân (dấu chấm)."));
        } else {
            score = new BigDecimal(scoreText);
            if (score.signum() <= 0) errors.add(new RowError(line, "Điểm phải lớn hơn 0."));
        }
        int last = row.size() - 1;
        while (last >= 4 && row.get(last).isBlank()) last--;
        var options = new ArrayList<AnswerOptionRequest>();
        for (int i = 4; i <= last; i++) {
            String raw = row.get(i);
            boolean correct = raw.startsWith("*");
            String value = correct ? raw.substring(1) : raw;
            if (value.isBlank() || value.length() > 1000) errors.add(new RowError(line, "Đáp án " + (i - 3) + " cần 1–1000 ký tự."));
            options.add(AnswerOptionRequest.builder().content(value).isCorrect(correct).build());
        }
        if (options.size() < 2) errors.add(new RowError(line, "Cần ít nhất 2 đáp án."));
        long correct = options.stream().filter(AnswerOptionRequest::getIsCorrect).count();
        if (type == QuestionType.MULTIPLE_CHOICE ? correct < 1 : correct != 1) {
            errors.add(new RowError(line, type == QuestionType.MULTIPLE_CHOICE
                    ? "Câu nhiều lựa chọn cần ít nhất 1 đáp án đúng (dấu *)."
                    : "Câu một lựa chọn / Đúng-Sai cần đúng 1 đáp án đúng (dấu *)."));
        }
        if (type == QuestionType.TRUE_FALSE && options.size() != 2) errors.add(new RowError(line, "Câu Đúng-Sai cần đúng 2 đáp án."));
        if (errors.size() == before) requests.add(CreateQuestionRequest.builder()
                .type(type).content(content).score(score).explanation(explanation).options(options).build());
    }
}
