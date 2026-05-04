package com.example.kserverproject.common.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND("CS_001", "존재하지 않는 유저입니다."),
    DUPLICATE_EMAIL("CS_002", "중복된 이메일 입니다."),
    INVALID_LOGIN_CREDENTIALS("CS_003", "로그인 정보가 일치하지 않습니다."),

    // Point
    INSUFFICIENT_POINTS_BALANCE("POINT_001", "포인트 잔액이 부족합니다."),
    INVALID_CHARGE_AMOUNT("POINT_002", "충전 금액은 0보다 커야합니다."),
    INVALID_REFUND_AMOUNT("POINT_003", "환불 금액은 0보다 커야합니다."),

    // Menu
    INVALID_MENU_PRICE("MENU_001", "메뉴 가격은 0보다 커야합니다."),
    MENU_NOT_FOUND("MENU_002", "존재하지 않는 메뉴입니다."),

    // Admin
    UNAUTHORIZED_ADMIN_ACCESS("ADMIN_001", "접근 권한이 없습니다."),

    // Token
    EXPIRED_TOKEN("AUTH_001", "유효하지 않은 토큰입니다."),
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다."),

    // Redis
    LOCK_ACQUISITION_FAILED("REDIS_001", "락 획득에 실패했습니다.");


    private final String code;
    private final String message;

}
