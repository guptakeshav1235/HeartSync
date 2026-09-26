package com.heartsync.verification.verification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.heartsync.verification.verification.entity.LivenessChallenge;

import jakarta.persistence.LockModeType;

public interface LivenessChallengeRepository extends JpaRepository<LivenessChallenge, Long> {

        Optional<LivenessChallenge> findByChallengeUuid(
                        UUID challengeUuid);

        long countByVerificationUserUuid(
                        UUID userUuid);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT lc
                        FROM LivenessChallenge lc
                        JOIN FETCH lc.verification fv
                        WHERE lc.challengeUuid = :challengeUuid
                        """)
        Optional<LivenessChallenge> findByChallengeUuidForUpdate(
                        @Param("challengeUuid") UUID challengeUuid);
}
