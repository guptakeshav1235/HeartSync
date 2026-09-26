package com.heartsync.verification.verification.service;

import java.util.List;
import java.util.UUID;

import com.heartsync.verification.verification.dto.response.StartVerificationResponse;
import com.heartsync.verification.verification.dto.response.VerificationResultResponse;

public interface VerificationService {

        StartVerificationResponse startVerification(
                        UUID userUuid,
                        UUID sessionUuid,
                        byte[] profileImage);

        VerificationResultResponse completeVerification(
                        UUID userUuid,
                        UUID verificationUuid,
                        UUID challengeUuid,
                        String nonce,
                        byte[] selfie,
                        List<byte[]> livenessFrames);

        VerificationResultResponse getVerification(
                        UUID userUuid,
                        UUID verificationUuid);
}
