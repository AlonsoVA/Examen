package com.urbangardens.parcel.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiniteNumberValidatorTest {

    private final FiniteNumberValidator validator = new FiniteNumberValidator();

    @Test
    @DisplayName("Acepta null y números finitos, y rechaza NaN e infinitos")
    void shouldRejectNonFiniteNumbers() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(22.5, null)).isTrue();
        assertThat(validator.isValid(0, null)).isTrue();
        assertThat(validator.isValid(Double.NaN, null)).isFalse();
        assertThat(validator.isValid(Double.POSITIVE_INFINITY, null)).isFalse();
        assertThat(validator.isValid(Double.NEGATIVE_INFINITY, null)).isFalse();
    }
}
