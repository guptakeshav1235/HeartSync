package com.heartsync.common.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static LocalDateTime addDays(int days) {
        return nowUtc().plusDays(days);
    }

    public static LocalDateTime addHours(int hours) {
        return nowUtc().plusHours(hours);
    }

    public static LocalDateTime addMinutes(int minutes) {
        return nowUtc().plusMinutes(minutes);
    }

    public static boolean isExpired(LocalDateTime expiryTime) {
        return nowUtc().isAfter(expiryTime);
    }
}
