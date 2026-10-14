package com.urbangardens.monitoring.dto;

import com.urbangardens.monitoring.validation.FiniteNumber;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Mediciones de una parcela.
 * soilMoisture: porcentaje, de 0 a 100.
 * temperature: grados Celsius, de -40 a 60.
 * ph: escala de pH, de 0 a 14.
 * nitrogen, phosphorus y potassium: mg/kg de suelo, no negativos.
 * airQuality: índice de calidad del aire (AQI), no negativo.
 */
public record SensorReadingRequest(
        @NotNull(message = "El identificador de la parcela es obligatorio.")
        @Positive(message = "El identificador de la parcela debe ser un número positivo.")
        Long parcelId,

        @NotNull(message = "La humedad del suelo es obligatoria.")
        @FiniteNumber(message = "La humedad del suelo debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "La humedad del suelo debe estar entre 0 y 100 por ciento.")
        @DecimalMax(value = "100.0", message = "La humedad del suelo debe estar entre 0 y 100 por ciento.")
        Double soilMoisture,

        @NotNull(message = "La temperatura es obligatoria.")
        @FiniteNumber(message = "La temperatura debe ser un número finito.")
        @DecimalMin(value = "-40.0", message = "La temperatura debe estar entre -40 y 60 grados Celsius.")
        @DecimalMax(value = "60.0", message = "La temperatura debe estar entre -40 y 60 grados Celsius.")
        Double temperature,

        @NotNull(message = "El pH es obligatorio.")
        @FiniteNumber(message = "El pH debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "El pH debe estar entre 0 y 14.")
        @DecimalMax(value = "14.0", message = "El pH debe estar entre 0 y 14.")
        Double ph,

        @NotNull(message = "El nitrógeno es obligatorio.")
        @FiniteNumber(message = "El nitrógeno debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "El nitrógeno no puede ser negativo.")
        Double nitrogen,

        @NotNull(message = "El fósforo es obligatorio.")
        @FiniteNumber(message = "El fósforo debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "El fósforo no puede ser negativo.")
        Double phosphorus,

        @NotNull(message = "El potasio es obligatorio.")
        @FiniteNumber(message = "El potasio debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "El potasio no puede ser negativo.")
        Double potassium,

        @NotNull(message = "La calidad del aire es obligatoria.")
        @FiniteNumber(message = "La calidad del aire debe ser un número finito.")
        @DecimalMin(value = "0.0", message = "La calidad del aire no puede ser negativa.")
        Double airQuality
) {
}
