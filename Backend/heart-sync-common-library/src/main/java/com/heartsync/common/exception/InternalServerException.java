package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

public class InternalServerException extends BaseException {
    public InternalServerException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }

}
