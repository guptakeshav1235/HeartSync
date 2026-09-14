package com.heartsync.auth.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.heartsync.auth.auth.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionUuid(UUID sessionUuid);

    List<UserSession> findAllByUserIdAndActiveTrue(Long userId);

    Optional<UserSession> findByUserIdAndDeviceIdAndActiveTrue(
            Long userId,
            String deviceId
    );
}
