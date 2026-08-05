package com.heartsync.common.util;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class DateTimeUtilsTest {

    @Test
    void shouldAddMinutes() {
        LocalDateTime now = DateTimeUtils.nowUtc();
        LocalDateTime future = DateTimeUtils.addMinutes(5);
        assertTrue(future.isAfter(now));
    }

    @Test
    void shouldDetectExpiredDate() {
        LocalDateTime expired = DateTimeUtils.nowUtc().minusMinutes(5);
        assertTrue(DateTimeUtils.isExpired(expired));
    }

    @Test
    void shouldDetectNonExpiredDate() {
        LocalDateTime future = DateTimeUtils.nowUtc().plusMinutes(5);
        assertFalse(DateTimeUtils.isExpired(future));
    }
}
