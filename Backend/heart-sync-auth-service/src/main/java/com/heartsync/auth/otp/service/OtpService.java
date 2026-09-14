package com.heartsync.auth.otp.service;

import com.heartsync.auth.otp.dto.request.SendOtpRequest;
import com.heartsync.auth.otp.dto.request.VerifyOtpRequest;
import com.heartsync.auth.otp.dto.response.OtpResponse;

public interface OtpService {

    OtpResponse sendOtp(SendOtpRequest request);

    void verifyOtp(VerifyOtpRequest request);
    
}
