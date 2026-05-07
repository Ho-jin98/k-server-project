package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Service 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {}", e.getMessage());
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
        log.warn("MethodArgumentNotValidException: {}", errorMessage);
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_REQUEST", errorMessage));
    }

    // 최후의 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: ", e);
        return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_SERVER_ERROR", e.getMessage()));
    }

    // 데이터 무결성 위반 예외처리
    // 만약, 두명의 관리자 우연히 똑같은 메뉴 이름으로 수정을 했을 경우
    // 500에러 반환이 아닌 정확한 에러를 내려주기 위해서 헨들러 적용
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {

        String errorMessage = e.getMessage();

        if (errorMessage != null && errorMessage.contains("uk_menu_name")) {
            log.warn("MenuNameDuplicateException: {}", errorMessage);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(
                            ErrorCode.DUPLICATE_MENU_NAME.name(),
                            ErrorCode.DUPLICATE_MENU_NAME.getMessage()));
        }
        log.warn("DataIntegrityViolationException: ", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("DATA_INTEGRITY_ERROR", e.getMessage()));
    }
}
