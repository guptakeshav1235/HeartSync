package com.heartsync.common.util;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String phoneNumber) {

        if (phoneNumber == null) {
            return null;
        }

        return phoneNumber
                .replace(" ", "")
                .replace("-", "");
    }
}
