package com.heartsync.verification.verification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.heartsync.verification.verification.enums.LivenessChallengeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;


@Getter 
@Setter 
@Entity 
@Table (
        name = "liveness_challenge",
        indexes = {
                @Index(
                        name = "idx_liveness_challenge_uuid",
                        columnList = "challenge_uuid"
                ),
                @Index(
                        name = "idx_liveness_verification",
                        columnList = "verification_id"
                )
        }
)
public class LivenessChallenge {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (
            name = "challenge_uuid",
            nullable = false,
            unique = true,
            length = 36
    )
    private UUID challengeUuid;

    @ManyToOne (
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn (
            name = "verification_id",
            nullable = false
    )
    private FaceVerification verification;

    @Column(
            name = "nonce_hash",
            nullable = false,
            length = 64
    )
    private String nonceHash;

    @Column(
            name = "action_sequence",
            nullable = false,
            length = 255
    )
    private String actionSequence;

    @Enumerated (EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private LivenessChallengeStatus status;

    @Column(
            name = "attempt_number",
            nullable = false
    )
    private Integer attemptNumber;

    @Column(
            name = "evidence_reference",
            length = 500
    )
    private String evidenceReference;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(name = "client_completed_at")
    private LocalDateTime clientCompletedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist 
    protected void onCreate() {

        if (challengeUuid == null) {
            challengeUuid = UUID.randomUUID();
        }

        if (status == null) {
            status =
                    LivenessChallengeStatus.CREATED;
        }

        createdAt = LocalDateTime.now();
    }
}
