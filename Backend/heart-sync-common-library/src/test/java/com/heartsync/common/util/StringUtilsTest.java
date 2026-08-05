package com.heartsync.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    void shouldReturnTruewhenStringIsNull() {
        assertTrue(StringUtils.isNullOrEmpty(null));
    }

    @Test
    void shouldReturnTrueWhenStringIsEmpty() {
        assertTrue(StringUtils.isNullOrEmpty(""));
    }

    @Test
    void shouldReturnTrueWhenStringContainsOnlySpaces() {
        assertTrue(StringUtils.isNullOrEmpty("   "));
    }

    @Test
    void shouldReturnFalseWhenStringHasValue() {
        assertFalse(StringUtils.isNullOrEmpty("HeartSync"));
    }

    @Test
    void shouldTrimSpaces() {
        assertEquals("Hello", StringUtils.trim("   Hello   "));
    }
}
