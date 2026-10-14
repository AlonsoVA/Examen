package com.urbangardens.parcel.service.impl;

import com.urbangardens.parcel.dto.FieldErrorDetail;
import com.urbangardens.parcel.dto.LocationDto;
import com.urbangardens.parcel.dto.ParcelRequest;
import com.urbangardens.parcel.dto.ParcelResponse;
import com.urbangardens.parcel.entity.Location;
import com.urbangardens.parcel.entity.Parcel;
import com.urbangardens.parcel.exception.InvalidDataException;
import com.urbangardens.parcel.exception.ResourceNotFoundException;
import com.urbangardens.parcel.repository.ParcelRepository;
import com.urbangardens.parcel.service.ParcelService;
import com.urbangardens.parcel.util.InputSanitizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;
    private final Validator validator;

    public ParcelServiceImpl(ParcelRepository parcelRepository, Validator validator) {
        this.parcelRepository = parcelRepository;
        this.validator = validator;
    }

    @Override
    @Transactional
    public ParcelResponse create(ParcelRequest request) {
        if (request == null) {
            throw new InvalidDataException("Los datos proporcionados no son válidos.");
        }

        ParcelRequest normalized = new ParcelRequest(
                InputSanitizer.normalize(request.name(), "name"),
                request.location(),
                request.soilType(),
                InputSanitizer.normalize(request.ownerOrLead(), "ownerOrLead"));
        rejectIfInvalid(normalized);

        Parcel parcel = new Parcel();
        parcel.setName(normalized.name());
        parcel.setLocation(new Location(normalized.location().latitude(), normalized.location().longitude()));
        parcel.setSoilType(normalized.soilType());
        parcel.setOwnerOrLead(normalized.ownerOrLead());
        parcel.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        return toResponse(parcelRepository.save(parcel));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelResponse> findAll() {
        return parcelRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelResponse findById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidDataException("El identificador debe ser un número positivo.");
        }
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la parcela con ID " + id + "."));
        return toResponse(parcel);
    }

    private void rejectIfInvalid(ParcelRequest request) {
        Set<ConstraintViolation<ParcelRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        List<FieldErrorDetail> details = violations.stream()
                .map(violation -> new FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        throw new InvalidDataException("Los datos proporcionados no son válidos.", details);
    }

    private ParcelResponse toResponse(Parcel parcel) {
        Location location = parcel.getLocation();
        return new ParcelResponse(
                parcel.getId(),
                parcel.getName(),
                new LocationDto(location.getLatitude(), location.getLongitude()),
                parcel.getSoilType(),
                parcel.getOwnerOrLead(),
                parcel.getCreatedAt());
    }
}
