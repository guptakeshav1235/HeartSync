package com.heartsync.auth.otp.dto.request;

import com.heartsync.auth.otp.enums.OtpPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{9,14}$",
                message = "Invalid phone number"
        )
        String phoneNumber,

        OtpPurpose purpose

) {

}
