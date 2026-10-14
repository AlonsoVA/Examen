package com.urbangardens.monitoring.controller;

import com.urbangardens.monitoring.dto.SensorReadingRequest;
import com.urbangardens.monitoring.dto.SensorReadingResponse;
import com.urbangardens.monitoring.service.SensorReadingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/readings")
@Validated
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    public SensorReadingController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    @PostMapping
    public ResponseEntity<SensorReadingResponse> create(@Valid @RequestBody SensorReadingRequest request) {
        SensorReadingResponse response = sensorReadingService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<List<SensorReadingResponse>> findByParcel(
            @PathVariable
            @Positive(message = "El identificador de la parcela debe ser un número positivo.") Long parcelId) {
        return ResponseEntity.ok(sensorReadingService.findByParcel(parcelId));
    }

    @GetMapping("/parcel/{parcelId}/latest")
    public ResponseEntity<SensorReadingResponse> findLatest(
            @PathVariable
            @Positive(message = "El identificador de la parcela debe ser un número positivo.") Long parcelId) {
        return ResponseEntity.ok(sensorReadingService.findLatest(parcelId));
    }
}
