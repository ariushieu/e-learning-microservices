package com.hunre.quizservice.service;

import java.util.List;

public class QuestionImportException extends RuntimeException {
    public record RowError(long line, String message) {}
    private final List<RowError> errors;

    public QuestionImportException(List<RowError> errors) {
        super("Không nhập câu hỏi nào. Vui lòng sửa các lỗi trong file CSV.");
        this.errors = List.copyOf(errors);
    }

    public QuestionImportException(long line, String message) {
        this(List.of(new RowError(line, message)));
    }

    public List<RowError> errors() { return errors; }
}
