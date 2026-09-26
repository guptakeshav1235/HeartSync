package com.heartsync.verification.verification.enums;

public enum LivenessFailureReason {
    NONE,

    TOO_FEW_FRAMES,

    TOO_MANY_FRAMES,

    INVALID_IMAGE,

    NO_FACE,

    MULTIPLE_FACES,

    SPOOF_DETECTED
}
