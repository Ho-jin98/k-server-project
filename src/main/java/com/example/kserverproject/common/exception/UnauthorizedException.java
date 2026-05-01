package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.exception.enums.ErrorCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
