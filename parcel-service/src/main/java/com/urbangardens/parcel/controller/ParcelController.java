package com.urbangardens.parcel.controller;

import com.urbangardens.parcel.dto.ParcelRequest;
import com.urbangardens.parcel.dto.ParcelResponse;
import com.urbangardens.parcel.service.ParcelService;
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
@RequestMapping("/api/v1/parcels")
@Validated
public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @PostMapping
    public ResponseEntity<ParcelResponse> create(@Valid @RequestBody ParcelRequest request) {
        ParcelResponse response = parcelService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ParcelResponse>> findAll() {
        return ResponseEntity.ok(parcelService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParcelResponse> findById(
            @PathVariable
            @Positive(message = "El identificador debe ser un número positivo.") Long id) {
        return ResponseEntity.ok(parcelService.findById(id));
    }
}
