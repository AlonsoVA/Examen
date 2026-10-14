package com.urbangardens.monitoring.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiniteNumberValidatorTest {

    private final FiniteNumberValidator validator = new FiniteNumberValidator();

    @Test
    @DisplayName("Rechaza NaN e infinitos")
    void shouldRejectNonFiniteNumbers() {
        assertThat(validator.isValid(6.8, null)).isTrue();
        assertThat(validator.isValid(Double.NaN, null)).isFalse();
        assertThat(validator.isValid(Double.POSITIVE_INFINITY, null)).isFalse();
    }
}
