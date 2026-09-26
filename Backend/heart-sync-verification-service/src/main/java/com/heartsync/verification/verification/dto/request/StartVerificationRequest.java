package com.heartsync.verification.verification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StartVerificationRequest(

        @NotBlank 
        String profileImageReference
) {
}
