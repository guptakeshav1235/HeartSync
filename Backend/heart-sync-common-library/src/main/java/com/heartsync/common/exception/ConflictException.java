package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

}
