package com.heartsync.verification.verification.service.implementation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartsync.verification.verification.service.FaceMatchingService;
import com.heartsync.verification.verification.service.LivenessChallengeService;
import com.heartsync.verification.verification.service.LivenessService;
import com.heartsync.verification.verification.service.VerificationService;

import lombok.RequiredArgsConstructor;

import com.heartsync.common.exception.BadRequestException;
import com.heartsync.common.exception.ResourceNotFoundException;
import com.heartsync.verification.storage.VerificationStorageService;
import com.heartsync.verification.verification.dto.response.LivenessChallengeResponse;
import com.heartsync.verification.verification.dto.response.StartVerificationResponse;
import com.heartsync.verification.verification.dto.response.VerificationResultResponse;
import com.heartsync.verification.verification.entity.FaceVerification;
import com.heartsync.verification.verification.entity.LivenessChallenge;
import com.heartsync.verification.verification.enums.VerificationStatus;
import com.heartsync.verification.verification.model.FaceMatchResult;
import com.heartsync.verification.verification.model.LivenessResult;
import com.heartsync.verification.verification.repository.FaceVerificationRepository;

@Service 
@RequiredArgsConstructor 
public class VerificationServiceImpl implements VerificationService {

    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);

    private final FaceVerificationRepository faceVerificationRepository;

    private final LivenessChallengeService livenessChallengeService;

    private final LivenessService livenessService;

    private final FaceMatchingService faceMatchingService;

    private final VerificationStorageService verificationStorageService;

    @Override
    @Transactional 
    public StartVerificationResponse startVerification(
            UUID userUuid,
            UUID sessionUuid,
            byte[] profileImage) {

        validateStartRequest(
                userUuid,
                sessionUuid,
                profileImage);

        /*
         * Generate UUID before saving because
         * our storage structure uses it.
         */
        UUID verificationUuid = UUID.randomUUID();

        String profileImageReference = null;

        try {

            /*
             * STEP 1:
             * Store profile image.
             */
            profileImageReference = verificationStorageService
                    .storeProfileImage(
                            verificationUuid,
                            profileImage);

            /*
             * STEP 2:
             * Create verification database record.
             */
            FaceVerification verification = new FaceVerification();

            verification.setVerificationUuid(
                    verificationUuid);

            verification.setUserUuid(
                    userUuid);

            verification.setSessionUuid(
                    sessionUuid);

            verification.setProfileImageReference(
                    profileImageReference);

            verification.setStatus(
                    VerificationStatus.PENDING);

            verification.setAttemptCount(
                    0);

            verification.setExpiresAt(
                    LocalDateTime.now()
                            .plus(
                                    VERIFICATION_TTL));

            faceVerificationRepository.save(
                    verification);

            /*
             * STEP 3:
             * Generate random liveness challenge.
             *
             * This will also update verification:
             *
             * PENDING -> IN_PROGRESS
             */
            LivenessChallengeResponse challenge = livenessChallengeService
                    .createChallenge(
                            verificationUuid,
                            userUuid);

            /*
             * STEP 4:
             * Send challenge information back
             * to the caller.
             */
            return new StartVerificationResponse(
                    verificationUuid,
                    challenge.challengeUuid(),
                    challenge.nonce(),
                    challenge.actions(),
                    challenge.expiresIn());

        } catch (RuntimeException exception) {

            /*
             * File system storage is not controlled
             * by the database transaction.
             *
             * If something fails after storing the
             * image, remove the orphaned image.
             */
            if (profileImageReference != null) {

                safelyDelete(
                        profileImageReference);
            }

            throw exception;
        }
    }

    @Override
    @Transactional
    public VerificationResultResponse completeVerification(
            UUID userUuid,
            UUID verificationUuid,
            UUID challengeUuid,
            String nonce,
            byte[] selfie,
            List<byte[]> livenessFrames) {

        /*
         * STEP 1:
         * Basic request validation.
         */
        validateCompleteRequest(
                userUuid,
                verificationUuid,
                challengeUuid,
                nonce,
                selfie,
                livenessFrames);

        /*
         * STEP 2:
         * Find verification and ensure that it
         * belongs to the authenticated user.
         */
        FaceVerification verification = getOwnedVerification(
                userUuid,
                verificationUuid);

        /*
         * STEP 3:
         * Verify current verification status.
         */
        validateVerificationState(
                verification);

        /*
         * STEP 4:
         * Validate challenge:
         *
         * - challenge exists
         * - belongs to verification
         * - belongs to user
         * - status = CREATED
         * - not expired
         * - nonce matches
         *
         * Then consume it:
         *
         * CREATED -> CLIENT_COMPLETED
         */
        LivenessChallenge challenge = livenessChallengeService
                .validateAndConsume(
                        userUuid,
                        verificationUuid,
                        challengeUuid,
                        nonce);

        /*
         * STEP 5:
         * Perform SERVER-SIDE liveness /
         * anti-spoof verification.
         */
        LivenessResult livenessResult = livenessService.verify(
                livenessFrames);

        verification.setLivenessPassed(
                livenessResult.passed());

        verification.setLivenessScore(
                livenessResult.score());

        verification.setLivenessModelVersion(
                livenessResult.modelVersion());

        /*
         * Liveness failed.
         *
         * Do NOT perform face matching.
         */
        if (!livenessResult.passed()) {

            livenessChallengeService
                    .markFailed(
                            challenge);

            failVerification(
                    verification,
                    livenessResult
                            .failureReason()
                            .name());

            return toResponse(
                    verification);
        }

        /*
         * Liveness passed.
         */
        livenessChallengeService
                .markVerified(
                        challenge);

        /*
         * STEP 6:
         * Read original profile photo.
         */
        byte[] profileImage = verificationStorageService
                .read(
                        verification
                                .getProfileImageReference());

        /*
         * STEP 7:
         * Compare:
         *
         * uploaded profile photo
         * VS
         * live captured selfie
         */
        FaceMatchResult faceMatchResult = faceMatchingService
                .compare(
                        profileImage,
                        selfie);

        verification.setFaceMatchScore(
                faceMatchResult.score());

        verification.setFaceModelVersion(
                faceMatchResult.modelVersion());

        /*
         * Face doesn't match.
         */
        if (!faceMatchResult.matched()) {

            failVerification(
                    verification,
                    faceMatchResult
                            .failureReason()
                            .name());

            return toResponse(
                    verification);
        }

        /*
         * STEP 8:
         *
         * Both passed:
         *
         * liveness = PASS
         * face match = PASS
         *
         * Store verification selfie.
         */
        String selfieReference = verificationStorageService
                .storeSelfie(
                        verificationUuid,
                        selfie);

        try {

            verification.setSelfieImageReference(
                    selfieReference);

            verification.setStatus(
                    VerificationStatus.VERIFIED);

            verification.setFailureReason(
                    null);

            verification.setVerifiedAt(
                    LocalDateTime.now());

            faceVerificationRepository.save(
                    verification);

        } catch (RuntimeException exception) {

            /*
             * If database operation fails,
             * don't leave an orphaned selfie.
             */
            safelyDelete(
                    selfieReference);

            throw exception;
        }

        /*
         * Important:
         *
         * We are NOT updating Auth Service here yet.
         *
         * The next backend step will implement
         * the reliable Outbox Event and internal
         * Auth Service notification.
         */
        return toResponse(
                verification);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResultResponse getVerification(
            UUID userUuid,
            UUID verificationUuid) {

        if (userUuid == null) {

            throw new BadRequestException(
                    "User UUID is required");
        }

        if (verificationUuid == null) {

            throw new BadRequestException(
                    "Verification UUID is required");
        }

        FaceVerification verification = getOwnedVerification(
                userUuid,
                verificationUuid);

        return toResponse(
                verification);
    }

    /*
     * ---------------------------------------------------
     * PRIVATE METHODS
     * ---------------------------------------------------
     */

    private FaceVerification getOwnedVerification(
            UUID userUuid,
            UUID verificationUuid) {

        FaceVerification verification = faceVerificationRepository
                .findByVerificationUuid(
                        verificationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Verification not found"));

        /*
         * Return the same error as "not found".
         *
         * We don't want to reveal whether another
         * user's verification UUID exists.
         */
        if (!verification
                .getUserUuid()
                .equals(userUuid)) {

            throw new ResourceNotFoundException(
                    "Verification not found");
        }

        return verification;
    }

    private void validateVerificationState(
            FaceVerification verification) {

        /*
         * First check time expiration.
         */
        if (!verification
                .getExpiresAt()
                .isAfter(
                        LocalDateTime.now())) {

            verification.setStatus(
                    VerificationStatus.EXPIRED);

            faceVerificationRepository.save(
                    verification);

            throw new BadRequestException(
                    "Verification expired");
        }

        /*
         * Already succeeded.
         */
        if (verification.getStatus() == VerificationStatus.VERIFIED) {

            throw new BadRequestException(
                    "Verification already completed");
        }

        /*
         * Previous attempt already failed.
         *
         * User should start a new verification
         * rather than replaying this attempt.
         */
        if (verification.getStatus() == VerificationStatus.FAILED) {

            throw new BadRequestException(
                    "Verification already failed");
        }

        if (verification.getStatus() == VerificationStatus.EXPIRED) {

            throw new BadRequestException(
                    "Verification expired");
        }

        /*
         * At this point we allow:
         *
         * PENDING
         * IN_PROGRESS
         */
    }

    private void failVerification(
            FaceVerification verification,
            String reason) {

        verification.setStatus(
                VerificationStatus.FAILED);

        verification.setFailureReason(
                reason);

        faceVerificationRepository.save(
                verification);
    }

    private VerificationResultResponse toResponse(
            FaceVerification verification) {

        return new VerificationResultResponse(
                verification
                        .getVerificationUuid(),

                verification
                        .getStatus(),

                Boolean.TRUE.equals(
                        verification
                                .getLivenessPassed()),

                verification
                        .getLivenessScore(),

                verification
                        .getFaceMatchScore(),

                verification
                        .getFailureReason(),

                verification
                        .getVerifiedAt());
    }

    private void validateStartRequest(
            UUID userUuid,
            UUID sessionUuid,
            byte[] profileImage) {

        if (userUuid == null) {

            throw new BadRequestException(
                    "User UUID is required");
        }

        if (sessionUuid == null) {

            throw new BadRequestException(
                    "Session UUID is required");
        }

        validateImage(
                profileImage,
                "Profile image is required");
    }

    private void validateCompleteRequest(
            UUID userUuid,
            UUID verificationUuid,
            UUID challengeUuid,
            String nonce,
            byte[] selfie,
            List<byte[]> livenessFrames) {

        if (userUuid == null) {

            throw new BadRequestException(
                    "User UUID is required");
        }

        if (verificationUuid == null) {

            throw new BadRequestException(
                    "Verification UUID is required");
        }

        if (challengeUuid == null) {

            throw new BadRequestException(
                    "Challenge UUID is required");
        }

        if (nonce == null
                || nonce.isBlank()) {

            throw new BadRequestException(
                    "Challenge nonce is required");
        }

        validateImage(
                selfie,
                "Selfie is required");

        if (livenessFrames == null
                || livenessFrames.isEmpty()) {

            throw new BadRequestException(
                    "Liveness frames are required");
        }

        for (byte[] frame : livenessFrames) {

            validateImage(
                    frame,
                    "Invalid liveness frame");
        }
    }

    private void validateImage(
            byte[] image,
            String message) {

        if (image == null
                || image.length == 0) {

            throw new BadRequestException(
                    message);
        }
    }

    private void safelyDelete(
            String reference) {

        try {

            verificationStorageService
                    .delete(
                            reference);

        } catch (RuntimeException ignored) {

            /*
             * We deliberately don't replace the
             * original exception with cleanup failure.
             *
             * Later we'll add proper logging here.
             */
        }
    }
}
