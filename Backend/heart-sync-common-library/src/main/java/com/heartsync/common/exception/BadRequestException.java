package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

}
