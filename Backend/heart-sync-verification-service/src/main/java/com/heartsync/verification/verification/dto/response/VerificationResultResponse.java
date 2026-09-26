package com.heartsync.verification.verification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.heartsync.verification.verification.enums.VerificationStatus;


public record VerificationResultResponse(

        UUID verificationUuid,

        VerificationStatus status,

        boolean livenessPassed,

        Double livenessScore,

        Double faceMatchScore,

        String failureReason,

        LocalDateTime verifiedAt) {

}
