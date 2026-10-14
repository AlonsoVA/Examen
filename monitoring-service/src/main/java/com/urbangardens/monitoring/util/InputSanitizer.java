package com.urbangardens.monitoring.util;

import com.urbangardens.monitoring.dto.FieldErrorDetail;
import com.urbangardens.monitoring.exception.InvalidDataException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Normaliza texto de entrada.
 * Las lecturas actuales son numéricas; esta clase aplica la misma política de saneamiento
 * si llega texto (por ejemplo, en una ampliación posterior).
 * Los saltos de línea y tabulaciones se tratan como espacios.
 * El resto de caracteres de control se rechaza.
 */
public final class InputSanitizer {

    private static final Pattern DISALLOWED_CONTROLS = Pattern.compile("[\\p{Cntrl}&&[^\\s]]");

    private InputSanitizer() {
    }

    public static String normalize(String value, String field) {
        if (value == null) {
            return null;
        }
        if (DISALLOWED_CONTROLS.matcher(value).find()) {
            throw new InvalidDataException(
                    "El texto contiene caracteres de control no permitidos.",
                    List.of(new FieldErrorDetail(
                            field,
                            "Elimine los caracteres de control e intente nuevamente.")));
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
