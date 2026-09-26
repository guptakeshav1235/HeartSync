package com.heartsync.verification.verification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.heartsync.verification.verification.entity.FaceVerification;
import com.heartsync.verification.verification.enums.VerificationStatus;

public interface FaceVerificationRepository extends JpaRepository<FaceVerification, Long> {

    Optional<FaceVerification> findByVerificationUuid(
            UUID verificationUuid);

    List<FaceVerification> findAllByUserUuidOrderByCreatedAtDesc(
            UUID userUuid);

    Optional<FaceVerification> findTopByUserUuidAndStatusOrderByCreatedAtDesc(
            UUID userUuid,
            VerificationStatus status);
            
}
