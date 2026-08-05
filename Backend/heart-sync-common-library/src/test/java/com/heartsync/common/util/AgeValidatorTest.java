package com.heartsync.common.util;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.heartsync.common.validation.AgeValidator;

public class AgeValidatorTest {

    private AgeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AgeValidator();
    }

    @Test
    void shouldAcceptAdultUser() {
        LocalDate dob = LocalDate.now().minusYears(25);
        assertTrue(validator.isValid(dob, null));
    }

    @Test
    void shouldRejectMinorUser() {
        LocalDate dob = LocalDate.now().minusYears(16);
        assertFalse(validator.isValid(dob, null));
    }

    @Test
    void shouldRejectNullDate() {
        assertFalse(validator.isValid(null, null));
    }
}
