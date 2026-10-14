package com.urbangardens.monitoring.util;

import com.urbangardens.monitoring.exception.InvalidDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputSanitizerTest {

    @Test
    @DisplayName("Normaliza espacios y conserva letras acentuadas")
    void shouldNormalizeText() {
        assertThat(InputSanitizer.normalize("  Huerto   del   Río  ", "note"))
                .isEqualTo("Huerto del Río");
    }

    @Test
    @DisplayName("Rechaza caracteres de control")
    void shouldRejectControlCharacters() {
        assertThatThrownBy(() -> InputSanitizer.normalize("dato\u0007", "note"))
                .isInstanceOf(InvalidDataException.class);
    }
}
