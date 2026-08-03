package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
    