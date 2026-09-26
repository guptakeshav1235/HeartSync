package com.heartsync.verification.verification.entity;

import com.heartsync.verification.verification.enums.VerificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "face_verification", indexes = {
        @Index(name = "idx_verification_user", columnList = "user_uuid"),
        @Index(name = "idx_verification_session", columnList = "session_uuid"),
        @Index(name = "idx_verification_uuid", columnList = "verification_uuid")
})
public class FaceVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_uuid", nullable = false, unique = true, length = 36)
    private UUID verificationUuid;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private UUID userUuid;

    @Column(name = "session_uuid", nullable = false, length = 36)
    private UUID sessionUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VerificationStatus status;

    @Column(name = "profile_image_reference", nullable = false, length = 500)
    private String profileImageReference;

    @Column(name = "selfie_image_reference", length = 500)
    private String selfieImageReference;

    @Column(name = "liveness_passed")
    private Boolean livenessPassed;

    @Column(name = "liveness_score")
    private Double livenessScore;

    @Column(name = "face_match_score")
    private Double faceMatchScore;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "face_model_version", length = 100)
    private String faceModelVersion;

    @Column(name = "liveness_model_version", length = 100)
    private String livenessModelVersion;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        if (verificationUuid == null) {
            verificationUuid = UUID.randomUUID();
        }

        if (status == null) {
            status = VerificationStatus.PENDING;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}