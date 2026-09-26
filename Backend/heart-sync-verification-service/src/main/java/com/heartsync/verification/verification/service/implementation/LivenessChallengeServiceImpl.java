package com.heartsync.verification.verification.service.implementation;

import com.heartsync.common.exception.BadRequestException;
import com.heartsync.common.exception.ResourceNotFoundException;
import com.heartsync.verification.verification.dto.response.LivenessChallengeResponse;
import com.heartsync.verification.verification.entity.FaceVerification;
import com.heartsync.verification.verification.entity.LivenessChallenge;
import com.heartsync.verification.verification.enums.LivenessAction;
import com.heartsync.verification.verification.enums.LivenessChallengeStatus;
import com.heartsync.verification.verification.enums.VerificationStatus;
import com.heartsync.verification.verification.repository.FaceVerificationRepository;
import com.heartsync.verification.verification.repository.LivenessChallengeRepository;
import com.heartsync.verification.verification.service.LivenessChallengeService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LivenessChallengeServiceImpl
                implements LivenessChallengeService {

        private static final int CHALLENGE_ACTION_COUNT = 3;

        private static final int NONCE_BYTES = 32;

        private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

        private final FaceVerificationRepository faceVerificationRepository;

        private final LivenessChallengeRepository livenessChallengeRepository;

        private final SecureRandom secureRandom = new SecureRandom();

        @Override
        @Transactional
        public LivenessChallengeResponse createChallenge(
                        UUID verificationUuid,
                        UUID userUuid) {

                FaceVerification verification = faceVerificationRepository
                                .findByVerificationUuid(
                                                verificationUuid)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Verification not found"));

                /*
                 * Security:
                 * user can create a challenge only for
                 * their own verification.
                 */
                if (!verification
                                .getUserUuid()
                                .equals(userUuid)) {

                        throw new ResourceNotFoundException(
                                        "Verification not found");
                }

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

                if (verification.getStatus() == VerificationStatus.VERIFIED) {

                        throw new BadRequestException(
                                        "Verification already completed");
                }

                List<LivenessAction> actions = generateActions();

                String rawNonce = generateNonce();

                LivenessChallenge challenge = new LivenessChallenge();

                challenge.setVerification(
                                verification);

                challenge.setNonceHash(
                                hashNonce(rawNonce));

                challenge.setActionSequence(
                                serializeActions(actions));

                challenge.setAttemptNumber(
                                verification.getAttemptCount() + 1);

                challenge.setStatus(
                                LivenessChallengeStatus.CREATED);

                challenge.setExpiresAt(
                                LocalDateTime.now()
                                                .plus(CHALLENGE_TTL));

                livenessChallengeRepository.save(
                                challenge);

                verification.setAttemptCount(
                                verification.getAttemptCount() + 1);

                verification.setStatus(
                                VerificationStatus.IN_PROGRESS);

                faceVerificationRepository.save(
                                verification);

                return new LivenessChallengeResponse(
                                challenge.getChallengeUuid(),
                                rawNonce,
                                actions,
                                CHALLENGE_TTL.toSeconds());
        }

        @Override
        @Transactional
        public LivenessChallenge validateAndConsume(
                        UUID userUuid,
                        UUID verificationUuid,
                        UUID challengeUuid,
                        String nonce) {

                /*
                 * Pessimistic lock prevents two requests
                 * from consuming the same challenge.
                 */
                LivenessChallenge challenge = livenessChallengeRepository
                                .findByChallengeUuidForUpdate(
                                                challengeUuid)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Liveness challenge not found"));

                FaceVerification verification = challenge.getVerification();

                /*
                 * Challenge must belong to the supplied
                 * verification.
                 */
                if (!verification
                                .getVerificationUuid()
                                .equals(verificationUuid)) {

                        throw new ResourceNotFoundException(
                                        "Liveness challenge not found");
                }

                /*
                 * Verification must belong to the
                 * authenticated user.
                 */
                if (!verification
                                .getUserUuid()
                                .equals(userUuid)) {

                        throw new ResourceNotFoundException(
                                        "Liveness challenge not found");
                }

                /*
                 * Challenge can only be consumed once.
                 */
                if (challenge.getStatus() != LivenessChallengeStatus.CREATED) {

                        throw new BadRequestException(
                                        "Liveness challenge is no longer active");
                }

                /*
                 * Check expiry.
                 */
                if (!challenge
                                .getExpiresAt()
                                .isAfter(
                                                LocalDateTime.now())) {

                        challenge.setStatus(
                                        LivenessChallengeStatus.EXPIRED);

                        livenessChallengeRepository.save(
                                        challenge);

                        throw new BadRequestException(
                                        "Liveness challenge expired");
                }

                /*
                 * Validate nonce.
                 */
                validateNonce(
                                nonce,
                                challenge.getNonceHash());

                /*
                 * Consume challenge immediately.
                 *
                 * It cannot be submitted again even if
                 * liveness subsequently fails.
                 */
                challenge.setStatus(
                                LivenessChallengeStatus.CLIENT_COMPLETED);

                challenge.setClientCompletedAt(
                                LocalDateTime.now());

                return livenessChallengeRepository.save(
                                challenge);
        }

        @Override
        @Transactional
        public void markVerified(
                        LivenessChallenge challenge) {

                if (challenge == null) {

                        throw new IllegalArgumentException(
                                        "Liveness challenge cannot be null");
                }

                if (challenge.getStatus() != LivenessChallengeStatus.CLIENT_COMPLETED) {

                        throw new BadRequestException(
                                        "Liveness challenge is not ready for verification");
                }

                challenge.setStatus(
                                LivenessChallengeStatus.VERIFIED);

                challenge.setVerifiedAt(
                                LocalDateTime.now());

                livenessChallengeRepository.save(
                                challenge);
        }

        @Override
        @Transactional
        public void markFailed(
                        LivenessChallenge challenge) {

                if (challenge == null) {

                        throw new IllegalArgumentException(
                                        "Liveness challenge cannot be null");
                }

                if (challenge.getStatus() != LivenessChallengeStatus.CLIENT_COMPLETED) {

                        throw new BadRequestException(
                                        "Liveness challenge is not ready for verification");
                }

                challenge.setStatus(
                                LivenessChallengeStatus.FAILED);

                livenessChallengeRepository.save(
                                challenge);
        }

        private void validateNonce(
                        String rawNonce,
                        String expectedHash) {

                if (rawNonce == null
                                || rawNonce.isBlank()) {

                        throw new BadRequestException(
                                        "Invalid challenge nonce");
                }

                String providedHash = hashNonce(rawNonce);

                boolean matches = MessageDigest.isEqual(
                                providedHash.getBytes(
                                                StandardCharsets.UTF_8),
                                expectedHash.getBytes(
                                                StandardCharsets.UTF_8));

                if (!matches) {

                        throw new BadRequestException(
                                        "Invalid challenge nonce");
                }
        }

        private List<LivenessAction> generateActions() {

                List<LivenessAction> available = new ArrayList<>(
                                Arrays.asList(
                                                LivenessAction.values()));

                Collections.shuffle(
                                available,
                                secureRandom);

                return new ArrayList<>(
                                available.subList(
                                                0,
                                                CHALLENGE_ACTION_COUNT));
        }

        private String generateNonce() {

                byte[] bytes = new byte[NONCE_BYTES];

                secureRandom.nextBytes(
                                bytes);

                return Base64
                                .getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(bytes);
        }

        private String serializeActions(
                        List<LivenessAction> actions) {

                return actions.stream()
                                .map(Enum::name)
                                .reduce(
                                                (left, right) -> left + "," + right)
                                .orElse("");
        }

        private String hashNonce(
                        String nonce) {

                try {

                        MessageDigest digest = MessageDigest.getInstance(
                                        "SHA-256");

                        byte[] hashed = digest.digest(
                                        nonce.getBytes(
                                                        StandardCharsets.UTF_8));

                        return HexFormat
                                        .of()
                                        .formatHex(hashed);

                } catch (NoSuchAlgorithmException exception) {

                        throw new IllegalStateException(
                                        "SHA-256 algorithm is unavailable",
                                        exception);
                }
        }
}