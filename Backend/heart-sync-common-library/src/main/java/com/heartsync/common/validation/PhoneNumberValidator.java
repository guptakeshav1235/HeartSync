package com.heartsync.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    @Override
    public boolean isValid(String phoneNumber,
                           ConstraintValidatorContext context) {

        if (phoneNumber == null) {
            return false;
        }

        return phoneNumber.matches("^\\d{10}$");
    }
}
