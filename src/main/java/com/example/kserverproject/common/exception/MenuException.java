package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.exception.enums.ErrorCode;

public class MenuException extends BusinessException {

    public MenuException(ErrorCode errorCode) {
        super(errorCode);
    }
}
