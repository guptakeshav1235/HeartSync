package com.heartsync.auth.auth.dto.response;

import java.util.UUID;

import com.heartsync.auth.auth.enums.AuthenticationStatus;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        String tokenType,

        long expiresIn,

        String userUuid,

        UUID sessionUuid,

        AuthenticationStatus status
        
) {

}
