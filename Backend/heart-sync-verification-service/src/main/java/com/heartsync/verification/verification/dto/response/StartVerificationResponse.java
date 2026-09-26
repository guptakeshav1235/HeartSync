package com.heartsync.verification.verification.dto.response;

import java.util.List;
import java.util.UUID;

import com.heartsync.verification.verification.enums.LivenessAction;

public record StartVerificationResponse(

        UUID verificationUuid,

        UUID challengeUuid,

        String nonce,

        List<LivenessAction> actions,

        long expiresIn
) {
}
