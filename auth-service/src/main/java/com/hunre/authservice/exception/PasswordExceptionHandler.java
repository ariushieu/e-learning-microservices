package com.hunre.authservice.exception;

import com.hunre.sharedcommon.dto.ErrorResponse;
import com.hunre.sharedcommon.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PasswordExceptionHandler {
    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectPassword(
            IncorrectCurrentPasswordException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.errorCode().httpStatus())
                .body(ErrorResponse.of(exception.errorCode().name(), exception.getMessage(),
                        request.getRequestURI(), List.of(
                                new FieldErrorDetail("currentPassword", exception.getMessage()))));
    }
}
