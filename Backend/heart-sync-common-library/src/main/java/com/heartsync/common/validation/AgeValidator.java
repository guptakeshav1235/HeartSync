package com.heartsync.common.validation;

import java.time.LocalDate;
import java.time.Period;

import com.heartsync.common.constants.ValidationConstants;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AgeValidator implements ConstraintValidator<ValidAge, LocalDate> {

    @Override
    public boolean isValid(LocalDate dateOfBirth,
                           ConstraintValidatorContext context) {

        if (dateOfBirth == null) {
            return false;
        }

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        return age >= ValidationConstants.MIN_AGE;
    }
}
