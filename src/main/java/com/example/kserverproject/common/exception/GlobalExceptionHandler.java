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

import java.nio.file.AccessDeniedException;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Service 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return buildErrorResponse(e.getErrorCode());
    }

    // Validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        log.warn("MethodArgumentNotValidException: {}", errorMessage);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST.name(), errorMessage));
    }


    // 데이터 무결성 위반 예외처리
    // 만약, 두명의 관리자 우연히 똑같은 메뉴 이름으로 수정을 했을 경우
    // 500에러 반환이 아닌 정확한 에러를 내려주기 위해서 헨들러 적용
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String errorMessage = e.getMessage();

        if (errorMessage != null && errorMessage.contains("uk_menu_name")) {
            log.warn("DataIntegrityViolationException - uk_menu_name: {}", errorMessage);
            return buildErrorResponse(ErrorCode.DUPLICATE_MENU_NAME);
        }

        if (errorMessage != null && errorMessage.contains("uk_order_menu")) {
            log.warn("DataIntegrityViolationException - uk_order_menu: {}", errorMessage);
            return buildErrorResponse(ErrorCode.DUPLICATE_MENU_IN_ORDER);
        }

        log.warn("DataIntegrityViolationException: ", e);
        return buildErrorResponse(ErrorCode.DATA_INTEGRITY_ERROR);
    }

    // 최후의 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: ", e);
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
//        log.warn("AccessDeniedException: ", e);
//        return buildErrorResponse(ErrorCode.ORDER_FORBIDDEN_ACCESS);
//    }

    // 공통 빌더
    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.name(), errorCode.getMessage()));
    }
}
