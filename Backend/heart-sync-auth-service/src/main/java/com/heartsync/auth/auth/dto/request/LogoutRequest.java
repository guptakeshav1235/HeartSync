package com.heartsync.auth.auth.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record LogoutRequest(

       @NotNull(message = "Session UUID is required")
        UUID sessionUuid
) {

}
