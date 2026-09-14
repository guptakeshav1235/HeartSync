package com.heartsync.auth.auth.dto.response;

public record RefreshTokenResult(

        String rawToken,
        long expiresIn) {
}
