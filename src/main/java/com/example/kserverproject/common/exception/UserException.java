package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.exception.enums.ErrorCode;

public class UserException extends BusinessException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
