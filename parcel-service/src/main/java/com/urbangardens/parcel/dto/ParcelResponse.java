package com.urbangardens.parcel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.urbangardens.parcel.entity.SoilType;

import java.time.LocalDateTime;

public record ParcelResponse(
        Long id,
        String name,
        LocationDto location,
        SoilType soilType,
        String ownerOrLead,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
