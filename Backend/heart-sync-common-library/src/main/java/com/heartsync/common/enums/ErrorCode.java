package com.heartsync.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Generic
    INTERNAL_SERVER_ERROR("HS-500", "Internal server error"),
    BAD_REQUEST("HS-400", "Bad request"),
    RESOURCE_NOT_FOUND("HS-404", "Resource not found"),
    UNAUTHORIZED("HS-401", "Unauthorized"),
    FORBIDDEN("HS-403", "Forbidden"),
    CONFLICT("HS-409", "Conflict"),

    // Validation
    VALIDATION_FAILED("HS-422", "Validation failed"),

    // Authentication
    INVALID_OTP("AUTH-001", "Invalid OTP"),
    OTP_EXPIRED("AUTH-002", "OTP expired"),
    INVALID_TOKEN("AUTH-003", "Invalid JWT Token"),
    TOKEN_EXPIRED("AUTH-004", "JWT Token expired"),

    // User
    USER_ALREADY_EXISTS("USER-001", "User already exists"),
    USER_NOT_FOUND("USER-002", "User not found"),

    // Verification
    FACE_VERIFICATION_FAILED("VERIFY-001", "Face verification failed");

    private final String code;

    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
