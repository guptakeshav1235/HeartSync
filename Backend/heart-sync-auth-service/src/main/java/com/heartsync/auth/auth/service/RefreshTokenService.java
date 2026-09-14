package com.heartsync.auth.auth.service;

import java.util.UUID;

import com.heartsync.auth.auth.dto.response.RefreshTokenResult;
import com.heartsync.auth.auth.entity.RefreshToken;
import com.heartsync.auth.auth.entity.UserSession;

public interface RefreshTokenService {

        // RefreshTokenResult createRefreshToken(
        // AuthUser user,
        // UserSession session);

        RefreshTokenResult createRefreshToken(
                        UserSession session);

        RefreshToken validateRefreshToken(
                        String rawRefreshToken);

        RefreshTokenResult rotateRefreshToken(
                        String rawRefreshToken);

        void revokeSessionRefreshTokens(
                        UUID sessionUuid);

        void revokeAllUserRefreshTokens(
                        Long userId);
}
