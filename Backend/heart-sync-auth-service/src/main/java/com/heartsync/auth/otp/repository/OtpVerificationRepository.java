package com.heartsync.auth.otp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.heartsync.auth.otp.entity.OtpVerification;
import com.heartsync.auth.otp.enums.OtpPurpose;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    Optional<OtpVerification> findTopByPhoneNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
        String phoneNumber,
        OtpPurpose purpose
    );
}
