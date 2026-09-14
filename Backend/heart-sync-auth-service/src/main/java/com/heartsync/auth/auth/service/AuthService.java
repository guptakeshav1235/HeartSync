package com.heartsync.auth.auth.service;

import java.util.UUID;

import com.heartsync.auth.auth.dto.request.LogoutRequest;
import com.heartsync.auth.auth.dto.request.RefreshTokenRequest;
import com.heartsync.auth.auth.dto.response.AuthResponse;
import com.heartsync.auth.auth.service.model.ClientContext;
import com.heartsync.auth.otp.dto.request.VerifyOtpRequest;

public interface AuthService {

    AuthResponse authenticate(
            VerifyOtpRequest request,
            ClientContext clientContext);

    AuthResponse completeImageVerification(
            UUID userUuid,
            UUID sessionUuid);

    AuthResponse refreshToken(
            RefreshTokenRequest request);

    void logout(
            LogoutRequest request);

}
