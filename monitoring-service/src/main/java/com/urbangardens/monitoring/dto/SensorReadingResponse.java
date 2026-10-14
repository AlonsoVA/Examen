package com.urbangardens.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SensorReadingResponse(
        Long id,
        Long parcelId,
        Double soilMoisture,
        Double temperature,
        Double ph,
        Double nitrogen,
        Double phosphorus,
        Double potassium,
        Double airQuality,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime recordedAt
) {
}
