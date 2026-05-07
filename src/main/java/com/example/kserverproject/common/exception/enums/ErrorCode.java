package com.example.kserverproject.common.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "중복된 이메일입니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "로그인 정보가 일치하지 않습니다."),

    // Point
    INSUFFICIENT_POINTS_BALANCE(HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야합니다."),
    INVALID_REFUND_AMOUNT(HttpStatus.BAD_REQUEST, "환불 금액은 0보다 커야합니다."),

    // Menu
    INVALID_MENU_PRICE(HttpStatus.BAD_REQUEST, "메뉴 가격은 0보다 커야합니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메뉴입니다."),
    DUPLICATE_MENU_NAME(HttpStatus.CONFLICT, "이미 존재하는 메뉴 이름입니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태입니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 주문에 대한 권한이 없습니다."),
    DUPLICATE_MENU_IN_ORDER(HttpStatus.CONFLICT, "주문에 중복된 메뉴가 포함되어 있습니다."),

    // Admin
    UNAUTHORIZED_ADMIN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    DATA_INTEGRITY_ERROR(HttpStatus.BAD_REQUEST, "데이터 무결성 제약을 위반했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Token
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    // Redis
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "락 획득에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
