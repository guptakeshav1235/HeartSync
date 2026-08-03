package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

}
