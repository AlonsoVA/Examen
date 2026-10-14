package com.urbangardens.monitoring.service;

import com.urbangardens.monitoring.client.ParcelClient;
import com.urbangardens.monitoring.dto.FieldErrorDetail;
import com.urbangardens.monitoring.dto.SensorReadingRequest;
import com.urbangardens.monitoring.dto.SensorReadingResponse;
import com.urbangardens.monitoring.entity.SensorReading;
import com.urbangardens.monitoring.exception.InvalidDataException;
import com.urbangardens.monitoring.exception.ResourceNotFoundException;
import com.urbangardens.monitoring.repository.SensorReadingRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final ParcelClient parcelClient;
    private final Validator validator;

    public SensorReadingService(SensorReadingRepository sensorReadingRepository,
                                ParcelClient parcelClient,
                                Validator validator) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.parcelClient = parcelClient;
        this.validator = validator;
    }

    @Transactional
    public SensorReadingResponse register(SensorReadingRequest request) {
        if (request == null) {
            throw new InvalidDataException("Los datos proporcionados no son válidos.");
        }
        rejectIfInvalid(request);
        parcelClient.ensureParcelExists(request.parcelId());

        SensorReading reading = new SensorReading();
        reading.setParcelId(request.parcelId());
        reading.setSoilMoisture(request.soilMoisture());
        reading.setTemperature(request.temperature());
        reading.setPh(request.ph());
        reading.setNitrogen(request.nitrogen());
        reading.setPhosphorus(request.phosphorus());
        reading.setPotassium(request.potassium());
        reading.setAirQuality(request.airQuality());
        reading.setRecordedAt(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        return toResponse(sensorReadingRepository.save(reading));
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> findByParcel(Long parcelId) {
        ensurePositiveParcelId(parcelId);
        parcelClient.ensureParcelExists(parcelId);
        return sensorReadingRepository.findByParcelIdOrderByRecordedAtDesc(parcelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorReadingResponse findLatest(Long parcelId) {
        ensurePositiveParcelId(parcelId);
        parcelClient.ensureParcelExists(parcelId);
        SensorReading reading = sensorReadingRepository.findFirstByParcelIdOrderByRecordedAtDesc(parcelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay lecturas registradas para la parcela con ID " + parcelId + "."));
        return toResponse(reading);
    }

    private void ensurePositiveParcelId(Long parcelId) {
        if (parcelId == null || parcelId <= 0) {
            throw new InvalidDataException("El identificador de la parcela debe ser un número positivo.");
        }
    }

    private void rejectIfInvalid(SensorReadingRequest request) {
        Set<ConstraintViolation<SensorReadingRequest>> violations = validator.validate(request);
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

    private SensorReadingResponse toResponse(SensorReading reading) {
        return new SensorReadingResponse(
                reading.getId(),
                reading.getParcelId(),
                reading.getSoilMoisture(),
                reading.getTemperature(),
                reading.getPh(),
                reading.getNitrogen(),
                reading.getPhosphorus(),
                reading.getPotassium(),
                reading.getAirQuality(),
                reading.getRecordedAt());
    }
}
