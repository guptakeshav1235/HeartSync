package com.heartsync.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.heartsync.common.validation.PhoneNumberValidator;

public class PhoneNumberValidatorTest {

    private PhoneNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhoneNumberValidator();
    }

    @Test
    void shouldAcceptValidPhoneNumber() {
        assertTrue(validator.isValid("9456939893", null));
    }

    @Test
    void shouldRejectNullPhoneNumber() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void shouldRejectShortPhoneNumber() {
        assertFalse(validator.isValid("12345", null));
    }

    @Test
    void shouldRejectAlphabeticPhoneNumber() {
        assertFalse(validator.isValid("abcd123456", null));
    }
}
