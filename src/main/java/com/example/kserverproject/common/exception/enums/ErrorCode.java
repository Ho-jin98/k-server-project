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

    // Token
    EXPIRED_TOKEN("AUTH_001", "유효하지 않은 토큰입니다."),
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.");


    private final String code;
    private final String message;

}
