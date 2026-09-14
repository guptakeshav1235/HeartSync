package com.heartsync.auth.auth.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartsync.auth.auth.entity.AuthUser;
import com.heartsync.auth.auth.entity.UserSession;
import com.heartsync.auth.auth.repository.UserSessionRepository;
import com.heartsync.auth.auth.service.UserSessionService;
import com.heartsync.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional
    public UserSession createSession(
            AuthUser user,
            String deviceId,
            String deviceName,
            String platform,
            String ipAddress) {

        UserSession session = new UserSession();

        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setDeviceName(deviceName);
        session.setPlatform(platform);
        session.setIpAddress(ipAddress);
        session.setActive(true);
        session.setLastLogin(LocalDateTime.now());

        return userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public UserSession createOrUpdateSession(
            AuthUser user,
            String deviceId,
            String deviceName,
            String platform,
            String ipAddress) {

        return userSessionRepository
                .findByUserIdAndDeviceIdAndActiveTrue(
                        user.getId(),
                        deviceId)
                .map(existingSession -> {

                    existingSession.setDeviceName(deviceName);
                    existingSession.setPlatform(platform);
                    existingSession.setIpAddress(ipAddress);
                    existingSession.setLastLogin(
                            LocalDateTime.now());

                    return userSessionRepository.save(
                            existingSession);
                })
                .orElseGet(() -> createSession(
                        user,
                        deviceId,
                        deviceName,
                        platform,
                        ipAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSession getSession(UUID sessionUuid) {

        return userSessionRepository
                .findBySessionUuid(sessionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> getActiveSessions(Long userId) {

        return userSessionRepository
                .findAllByUserIdAndActiveTrue(userId);
    }

    @Override
    @Transactional
    public void deactivateSession(UUID sessionUuid) {

        UserSession session = getSession(sessionUuid);

        session.setActive(false);

        userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void deactivateAllSessions(Long userId) {

        List<UserSession> sessions = userSessionRepository
                .findAllByUserIdAndActiveTrue(userId);

        for (UserSession session : sessions) {
            session.setActive(false);
        }

        userSessionRepository.saveAll(sessions);
    }
}
