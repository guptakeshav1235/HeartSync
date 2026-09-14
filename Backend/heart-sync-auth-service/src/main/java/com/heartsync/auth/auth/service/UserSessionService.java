package com.heartsync.auth.auth.service;

import java.util.List;
import java.util.UUID;

import com.heartsync.auth.auth.entity.AuthUser;
import com.heartsync.auth.auth.entity.UserSession;

public interface UserSessionService {

    UserSession createSession(
            AuthUser user,
            String deviceId,
            String deviceName,
            String platform,
            String ipAddress);

    UserSession createOrUpdateSession(
            AuthUser user,
            String deviceId,
            String deviceName,
            String platform,
            String ipAddress);

    UserSession getSession(UUID sessionUuid);

    List<UserSession> getActiveSessions(Long userId);

    void deactivateSession(UUID sessionUuid);

    void deactivateAllSessions(Long userId);
}
