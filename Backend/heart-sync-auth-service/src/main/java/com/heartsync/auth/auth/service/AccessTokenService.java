package com.heartsync.auth.auth.service;

import com.heartsync.auth.auth.dto.response.AccessTokenResult;
import com.heartsync.auth.auth.entity.AuthUser;
import com.heartsync.auth.auth.entity.UserSession;

public interface AccessTokenService {

    AccessTokenResult generateAuthenticatedToken(
            AuthUser user,
            UserSession session);

    AccessTokenResult generateOnboardingToken(
            AuthUser user,
            UserSession session);
}
