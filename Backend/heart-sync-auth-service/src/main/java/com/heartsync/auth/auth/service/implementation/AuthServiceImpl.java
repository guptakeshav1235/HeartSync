package com.heartsync.auth.auth.service.implementation;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartsync.auth.auth.dto.request.LogoutRequest;
import com.heartsync.auth.auth.dto.request.RefreshTokenRequest;
import com.heartsync.auth.auth.dto.response.AccessTokenResult;
import com.heartsync.auth.auth.dto.response.AuthResponse;
import com.heartsync.auth.auth.dto.response.RefreshTokenResult;
import com.heartsync.auth.auth.entity.AuthUser;
import com.heartsync.auth.auth.entity.RefreshToken;
import com.heartsync.auth.auth.entity.UserSession;
import com.heartsync.auth.auth.enums.AuthenticationStatus;
import com.heartsync.auth.auth.repository.AuthUserRepository;
import com.heartsync.auth.auth.service.AccessTokenService;
import com.heartsync.auth.auth.service.AuthService;
import com.heartsync.auth.auth.service.RefreshTokenService;
import com.heartsync.auth.auth.service.UserSessionService;
import com.heartsync.auth.auth.service.model.ClientContext;
import com.heartsync.auth.otp.dto.request.VerifyOtpRequest;
import com.heartsync.auth.otp.service.OtpService;
import com.heartsync.common.enums.AccountStatus;
import com.heartsync.common.enums.ErrorCode;
import com.heartsync.common.exception.BadRequestException;
import com.heartsync.common.exception.ForbiddenException;
import com.heartsync.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;

    private final OtpService otpService;

    private final UserSessionService userSessionService;

    private final RefreshTokenService refreshTokenService;

    private final AccessTokenService accessTokenService;

    @Override
    @Transactional
    public AuthResponse authenticate(
            VerifyOtpRequest request,
            ClientContext clientContext) {

        /*
         * Step 1:
         * Verify OTP first.
         */
        otpService.verifyOtp(request);

        /*
         * Step 2:
         * Find existing user or create a new one.
         */
        AuthUser user = findOrCreateUser(
                request.phoneNumber());

        /*
         * Step 3:
         * OTP succeeded, therefore phone number
         * is verified.
         */
        user.setPhoneVerified(true);

        validateAccount(user);

        /*
         * Step 4:
         * Create or reuse device session.
         */
        UserSession session = userSessionService
                .createOrUpdateSession(
                        user,
                        clientContext.deviceId(),
                        clientContext.deviceName(),
                        clientContext.platform(),
                        clientContext.ipAddress());

        /*
         * Step 5:
         * Mandatory image verification.
         */
        if (!Boolean.TRUE.equals(
                user.getImageVerified())) {

            AccessTokenResult onboardingToken = accessTokenService
                    .generateOnboardingToken(
                            user,
                            session);

            return new AuthResponse(
                    onboardingToken.token(),
                    null,
                    "Bearer",
                    onboardingToken.expiresIn(),
                    user.getUuid().toString(),
                    session.getSessionUuid(),
                    AuthenticationStatus.IMAGE_VERIFICATION_REQUIRED);
        }

        /*
         * Existing verified user:
         * create normal application tokens.
         */
        return createAuthenticatedResponse(
                user,
                session);
    }

    @Override
    @Transactional
    public AuthResponse completeImageVerification(
            UUID userUuid,
            UUID sessionUuid) {

        AuthUser user = authUserRepository
                .findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND));

        validateAccount(user);

        UserSession session = userSessionService
                .getSession(sessionUuid);

        /*
         * Security check:
         * Session must belong to this user.
         */
        if (!session.getUser()
                .getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    ErrorCode.INVALID_TOKEN);
        }

        if (!Boolean.TRUE.equals(
                session.getActive())) {

            throw new BadRequestException(
                    ErrorCode.INVALID_TOKEN);
        }

        /*
         * IMPORTANT:
         * This method should only be called after
         * the verification system has actually
         * confirmed the user's image.
         */
        user.setImageVerified(true);

        authUserRepository.save(user);

        /*
         * Now user is allowed full authentication.
         */
        return createAuthenticatedResponse(
                user,
                session);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(
            RefreshTokenRequest request) {

        /*
         * First validate current refresh token.
         */
        RefreshToken existingToken = refreshTokenService
                .validateRefreshToken(
                        request.refreshToken());

        UserSession session = existingToken.getSession();

        AuthUser user = session.getUser();

        validateAccount(user);

        /*
         * Image verification is mandatory.
         * A refresh token should never allow an
         * unverified user into the application.
         */
        if (!Boolean.TRUE.equals(
                user.getImageVerified())) {

            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN);
        }

        /*
         * Rotate R1 -> R2.
         */
        RefreshTokenResult rotatedToken = refreshTokenService
                .rotateRefreshToken(
                        request.refreshToken());

        /*
         * Generate new access JWT.
         */
        AccessTokenResult accessToken = accessTokenService
                .generateAuthenticatedToken(
                        user,
                        session);

        return new AuthResponse(
                accessToken.token(),
                rotatedToken.rawToken(),
                "Bearer",
                accessToken.expiresIn(),
                user.getUuid().toString(),
                session.getSessionUuid(),
                AuthenticationStatus.AUTHENTICATED);
    }

    @Override
    @Transactional
    public void logout(
            LogoutRequest request) {

        /*
         * Revoke every refresh token associated
         * with this login session.
         */
        refreshTokenService
                .revokeSessionRefreshTokens(
                        request.sessionUuid());

        /*
         * Disable the actual session.
         */
        userSessionService
                .deactivateSession(
                        request.sessionUuid());
    }

    private AuthUser findOrCreateUser(
            String phoneNumber) {

        return authUserRepository
                .findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {

                    AuthUser user = new AuthUser();

                    user.setPhoneNumber(
                            phoneNumber);

                    user.setPhoneVerified(true);

                    user.setImageVerified(false);

                    user.setAccountStatus(
                            AccountStatus.ACTIVE);

                    return authUserRepository
                            .save(user);
                });
    }

    private AuthResponse createAuthenticatedResponse(
            AuthUser user,
            UserSession session) {

        refreshTokenService
                .revokeSessionRefreshTokens(
                        session.getSessionUuid());

        AccessTokenResult accessToken = accessTokenService
                .generateAuthenticatedToken(
                        user,
                        session);

        RefreshTokenResult refreshToken = refreshTokenService
                .createRefreshToken(
                        session);

        return new AuthResponse(
                accessToken.token(),
                refreshToken.rawToken(),
                "Bearer",
                accessToken.expiresIn(),
                user.getUuid().toString(),
                session.getSessionUuid(),
                AuthenticationStatus.AUTHENTICATED);
    }

    private void validateAccount(
            AuthUser user) {

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {

            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN);
        }
    }
}
