package com.urbangardens.parcel.util;

import com.urbangardens.parcel.exception.InvalidDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputSanitizerTest {

    @Test
    @DisplayName("Recorta extremos, colapsa espacios y conserva acentos y apóstrofos")
    void shouldNormalizeLegitimateNames() {
        assertThat(InputSanitizer.normalize("  María   José  O'Connor-López  ", "name"))
                .isEqualTo("María José O'Connor-López");
        assertThat(InputSanitizer.normalize("Huerto\nNorte\tSur", "name"))
                .isEqualTo("Huerto Norte Sur");
    }

    @Test
    @DisplayName("Rechaza caracteres de control que no son espacios")
    void shouldRejectDisallowedControlCharacters() {
        assertThatThrownBy(() -> InputSanitizer.normalize("Huerto\u0000Norte", "name"))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("El texto contiene caracteres de control no permitidos.");
    }

    @Test
    @DisplayName("Devuelve null cuando el texto es null")
    void shouldKeepNull() {
        assertThat(InputSanitizer.normalize(null, "name")).isNull();
    }
}
