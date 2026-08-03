package com.heartsync.common.exception;

import com.heartsync.common.enums.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode, String message) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode; 
    }
}
