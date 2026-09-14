package com.heartsync.auth.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "user_session",
    indexes = {
            @Index(name = "idx_session_user", columnList = "user_id"),
            @Index(name = "idx_session_device", columnList = "device_id")
    }
)
public class UserSession extends BaseEntity{

    @Column(name = "session_uuid", nullable = false, unique = true, length = 36)
    private UUID sessionUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 255)
    private String deviceName;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_login", nullable = false)
    private LocalDateTime lastLogin;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (sessionUuid == null) {
            sessionUuid = UUID.randomUUID();
        }
        lastLogin = LocalDateTime.now();
    }
}
