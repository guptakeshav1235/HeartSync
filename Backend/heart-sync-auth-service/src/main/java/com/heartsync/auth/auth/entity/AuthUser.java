package com.heartsync.auth.auth.entity;

import java.util.UUID;

import com.heartsync.common.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "auth_user",
    uniqueConstraints = {
            @UniqueConstraint(name = "uk_phone_number", columnNames = "phone_number"),
            @UniqueConstraint(name = "uk_uuid", columnNames = "uuid")
    }
)
public class AuthUser extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 36)
    private UUID uuid;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    @Column(name = "image_verified", nullable = false)
    private Boolean imageVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
