package com.urbangardens.monitoring.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FiniteNumberValidator implements ConstraintValidator<FiniteNumber, Number> {

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Double.isFinite(value.doubleValue());
    }
}
