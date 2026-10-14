package com.urbangardens.parcel.dto;

import com.urbangardens.parcel.entity.SoilType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParcelRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 100, message = "El nombre debe contener entre 3 y 100 caracteres.")
        String name,

        @NotNull(message = "La ubicación es obligatoria.")
        @Valid
        LocationDto location,

        @NotNull(message = "El tipo de suelo es obligatorio.")
        SoilType soilType,

        @NotBlank(message = "El responsable es obligatorio.")
        @Size(min = 3, max = 100, message = "El responsable debe contener entre 3 y 100 caracteres.")
        String ownerOrLead
) {
}
