package com.heartsync.auth.auth.dto.response;

public record AccessTokenResult(

        String token,

        long expiresIn) {
}
