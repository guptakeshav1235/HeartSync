package com.heartsync.verification.verification.service;

import java.util.UUID;

import com.heartsync.verification.verification.dto.response.LivenessChallengeResponse;
import com.heartsync.verification.verification.entity.LivenessChallenge;

public interface LivenessChallengeService {

        LivenessChallengeResponse createChallenge(
                        UUID verificationUuid,
                        UUID userUuid);

        LivenessChallenge validateAndConsume(
                        UUID userUuid,
                        UUID verificationUuid,
                        UUID challengeUuid,
                        String nonce);

        void markVerified(
                        LivenessChallenge challenge);

        void markFailed(
                        LivenessChallenge challenge);
}
