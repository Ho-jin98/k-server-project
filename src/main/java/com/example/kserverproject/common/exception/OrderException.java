package com.example.kserverproject.common.exception;

import com.example.kserverproject.common.exception.enums.ErrorCode;

public class OrderException extends BusinessException {

    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
