package com.hunre.quizservice.controller;

import com.hunre.quizservice.service.QuestionImportException;
import com.hunre.quizservice.service.QuestionImportException.RowError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(-100)
public class QuestionImportExceptionHandler {
    public record ImportError(boolean success, String code, String message, String path,
                              Instant timestamp, List<RowError> errors) {}

    @ExceptionHandler(QuestionImportException.class)
    public ResponseEntity<ImportError> invalid(QuestionImportException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ImportError(false, "VALIDATION_FAILED", ex.getMessage(),
                request.getRequestURI(), Instant.now(), ex.errors()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ImportError> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return invalid(new QuestionImportException(1, "File tối đa 1 MB (1048576 byte)."), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ImportError> missing(MissingServletRequestPartException ex, HttpServletRequest request) {
        return invalid(new QuestionImportException(1, "Cần gửi file CSV trong trường file."), request);
    }
}
