package com.heartsync.common.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
