package com.urbangardens.parcel.dto;

import com.urbangardens.parcel.validation.FiniteNumber;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationDto(
        @NotNull(message = "La latitud es obligatoria.")
        @FiniteNumber(message = "La latitud debe ser un número finito.")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90.")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90.")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria.")
        @FiniteNumber(message = "La longitud debe ser un número finito.")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180.")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180.")
        Double longitude
) {
}
