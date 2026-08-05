package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class ValidationException extends  BaseException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException() {
        super(ErrorCode.VALIDATION_FAILED);
    }
}
