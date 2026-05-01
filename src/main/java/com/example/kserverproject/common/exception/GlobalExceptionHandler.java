package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Service 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.badRequest().body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage()));
    }

    // Validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("유효하지 않은 요청입니다.");
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_REQUEST", errorMessage));
    }

    // 최후의 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_SERVER_ERROR", e.getMessage()));
    }
}
