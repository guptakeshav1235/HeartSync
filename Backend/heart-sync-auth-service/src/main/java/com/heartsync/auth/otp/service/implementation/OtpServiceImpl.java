package com.heartsync.auth.otp.service.implementation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heartsync.auth.otp.dto.request.SendOtpRequest;
import com.heartsync.auth.otp.dto.request.VerifyOtpRequest;
import com.heartsync.auth.otp.dto.response.OtpResponse;
import com.heartsync.auth.otp.entity.OtpVerification;
import com.heartsync.auth.otp.repository.OtpVerificationRepository;
import com.heartsync.auth.otp.service.OtpService;
import com.heartsync.common.enums.ErrorCode;
import com.heartsync.common.exception.BadRequestException;
import com.heartsync.common.exception.InternalServerException;
import com.heartsync.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
        
        private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpVerificationRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional 
    public OtpResponse sendOtp(SendOtpRequest request) {

        String otp = generateOtp();

        OtpVerification otpVerification = new OtpVerification();

        otpVerification.setPhoneNumber(request.phoneNumber());
        otpVerification.setOtpHash(hashOtp(otp));
        otpVerification.setPurpose(request.purpose());
        otpVerification.setExpiresAt(
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        otpVerification.setVerified(false);
        otpVerification.setAttempts(0);

        otpVerificationRepository.save(otpVerification);

        /*
         * TODO:
         * Send OTP through SMS provider.
         *
         * We will integrate the SMS provider separately.
         */

        return new OtpResponse(
                "OTP sent successfully",
                OTP_EXPIRY_MINUTES * 60L
        );
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {

        OtpVerification otpVerification =
                otpVerificationRepository
                        .findTopByPhoneNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                                request.phoneNumber(),
                                request.purpose()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        ErrorCode.INVALID_OTP
                                )
                        );

        if (otpVerification.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new BadRequestException(
                    ErrorCode.OTP_EXPIRED
            );
        }

        if (otpVerification.getAttempts() >= MAX_ATTEMPTS) {

            throw new BadRequestException(
                    ErrorCode.INVALID_OTP
            );
        }

        otpVerification.setAttempts(
                otpVerification.getAttempts() + 1
        );

        String providedOtpHash = hashOtp(request.otp());

        boolean otpMatches = MessageDigest.isEqual(
                otpVerification.getOtpHash()
                        .getBytes(StandardCharsets.UTF_8),
                providedOtpHash
                        .getBytes(StandardCharsets.UTF_8)
        );

        if (!otpMatches) {

            otpVerificationRepository.save(otpVerification);

            throw new BadRequestException(
                    ErrorCode.INVALID_OTP
            );
        }

        otpVerification.setVerified(true);

        otpVerificationRepository.save(otpVerification);
    }

    private String generateOtp() {

        int otp = secureRandom.nextInt(900000) + 100000;

        return String.valueOf(otp);
    }

    private String hashOtp(String otp) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            otp.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(
                        String.format("%02x", b)
                );
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new InternalServerException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }
}
