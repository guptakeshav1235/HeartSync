package com.heartsync.auth.auth.service.model;

public record ClientContext(

    String deviceId,

    String deviceName,

    String platform,

    String ipAddress
    
) {
}
