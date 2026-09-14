package com.heartsync.auth.auth.service.implementation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartsync.auth.auth.dto.response.RefreshTokenResult;
import com.heartsync.auth.auth.entity.AuthUser;
import com.heartsync.auth.auth.entity.RefreshToken;
import com.heartsync.auth.auth.entity.UserSession;
import com.heartsync.auth.auth.repository.RefreshTokenRepository;
import com.heartsync.auth.auth.service.RefreshTokenService;
import com.heartsync.common.enums.AccountStatus;
import com.heartsync.common.enums.ErrorCode;
import com.heartsync.common.exception.BadRequestException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

        private static final int REFRESH_TOKEN_BYTES = 64;

        private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

        private final RefreshTokenRepository refreshTokenRepository;

        private final SecureRandom secureRandom = new SecureRandom();

        @Override
        @Transactional
        public RefreshTokenResult createRefreshToken(
                        UserSession session) {

                validateSession(session);

                return createAndPersistToken(session);
        }

        @Override
        @Transactional(readOnly = true)
        public RefreshToken validateRefreshToken(
                        String rawRefreshToken) {

                validateRawToken(rawRefreshToken);

                String tokenHash = hashToken(rawRefreshToken);

                RefreshToken refreshToken = refreshTokenRepository
                                .findByTokenHashAndRevokedFalse(tokenHash)
                                .orElseThrow(() -> new BadRequestException(
                                                ErrorCode.INVALID_TOKEN));

                validateStoredToken(refreshToken);

                return refreshToken;
        }

        @Override
        @Transactional
        public RefreshTokenResult rotateRefreshToken(
                        String rawRefreshToken) {

                validateRawToken(rawRefreshToken);

                String tokenHash = hashToken(rawRefreshToken);

                RefreshToken existingToken = refreshTokenRepository
                                .findActiveTokenForUpdate(tokenHash)
                                .orElseThrow(() -> new BadRequestException(
                                                ErrorCode.INVALID_TOKEN));

                validateStoredToken(existingToken);

                existingToken.setRevoked(true);
                existingToken.setRevokedAt(
                                LocalDateTime.now());

                refreshTokenRepository.save(existingToken);

                return createAndPersistToken(
                                existingToken.getSession());
        }

        @Override
        @Transactional
        public void revokeSessionRefreshTokens(
                        UUID sessionUuid) {

                refreshTokenRepository
                                .revokeAllBySessionUuid(
                                                sessionUuid,
                                                LocalDateTime.now());
        }

        @Override
        @Transactional
        public void revokeAllUserRefreshTokens(
                        Long userId) {

                refreshTokenRepository
                                .revokeAllByUserId(
                                                userId,
                                                LocalDateTime.now());
        }

        private RefreshTokenResult createAndPersistToken(
                        UserSession session) {

                String rawToken = generateRawToken();

                String tokenHash = hashToken(rawToken);

                LocalDateTime expiresAt = LocalDateTime.now()
                                .plus(REFRESH_TOKEN_TTL);

                RefreshToken refreshToken = new RefreshToken();

                refreshToken.setSession(session);
                refreshToken.setTokenHash(tokenHash);
                refreshToken.setExpiresAt(expiresAt);
                refreshToken.setRevoked(false);

                refreshTokenRepository.save(refreshToken);

                return new RefreshTokenResult(
                                rawToken,
                                REFRESH_TOKEN_TTL.toSeconds());
        }

        private void validateStoredToken(
                        RefreshToken refreshToken) {

                if (refreshToken.getExpiresAt()
                                .isBefore(LocalDateTime.now())
                                || refreshToken.getExpiresAt()
                                                .isEqual(LocalDateTime.now())) {

                        throw new BadRequestException(
                                        ErrorCode.TOKEN_EXPIRED);
                }

                UserSession session = refreshToken.getSession();

                if (!Boolean.TRUE.equals(
                                session.getActive())) {

                        throw new BadRequestException(
                                        ErrorCode.INVALID_TOKEN);
                }

                AuthUser user = session.getUser();

                if (user.getAccountStatus() != AccountStatus.ACTIVE) {

                        throw new BadRequestException(
                                        ErrorCode.INVALID_TOKEN);
                }
        }

        private void validateSession(
                        UserSession session) {

                if (session == null
                                || !Boolean.TRUE.equals(
                                                session.getActive())) {

                        throw new BadRequestException(
                                        ErrorCode.INVALID_TOKEN);
                }

                if (session.getUser() == null
                                || session.getUser()
                                                .getAccountStatus() != AccountStatus.ACTIVE) {

                        throw new BadRequestException(
                                        ErrorCode.INVALID_TOKEN);
                }
        }

        private void validateRawToken(
                        String rawRefreshToken) {

                if (rawRefreshToken == null
                                || rawRefreshToken.isBlank()) {

                        throw new BadRequestException(
                                        ErrorCode.INVALID_TOKEN);
                }
        }

        private String generateRawToken() {

                byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];

                secureRandom.nextBytes(randomBytes);

                return Base64
                                .getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(randomBytes);
        }

        private String hashToken(
                        String rawToken) {

                try {

                        MessageDigest digest = MessageDigest.getInstance(
                                        "SHA-256");

                        byte[] hashedBytes = digest.digest(
                                        rawToken.getBytes(
                                                        java.nio.charset.StandardCharsets.UTF_8));

                        return HexFormat
                                        .of()
                                        .formatHex(hashedBytes);

                } catch (NoSuchAlgorithmException exception) {

                        throw new IllegalStateException(
                                        "SHA-256 algorithm is not available",
                                        exception);
                }
        }
}
