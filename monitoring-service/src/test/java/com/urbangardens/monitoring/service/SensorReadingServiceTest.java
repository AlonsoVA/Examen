package com.urbangardens.monitoring.service;

import com.urbangardens.monitoring.client.ParcelClient;
import com.urbangardens.monitoring.dto.SensorReadingRequest;
import com.urbangardens.monitoring.dto.SensorReadingResponse;
import com.urbangardens.monitoring.entity.SensorReading;
import com.urbangardens.monitoring.exception.InvalidDataException;
import com.urbangardens.monitoring.exception.ResourceNotFoundException;
import com.urbangardens.monitoring.exception.ServiceUnavailableException;
import com.urbangardens.monitoring.repository.SensorReadingRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorReadingServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private ParcelClient parcelClient;

    private SensorReadingService sensorReadingService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sensorReadingService = new SensorReadingService(sensorReadingRepository, parcelClient, validator);
    }

    @Test
    @DisplayName("Registra una lectura después de comprobar que la parcela existe")
    void shouldRegisterReadingWhenParcelExists() {
        when(sensorReadingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> {
            SensorReading reading = invocation.getArgument(0);
            reading.setId(10L);
            return reading;
        });

        SensorReadingResponse response = sensorReadingService.register(validRequest(1L, 6.8));

        verify(parcelClient).ensureParcelExists(1L);
        ArgumentCaptor<SensorReading> captor = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingRepository).save(captor.capture());
        assertThat(captor.getValue().getPh()).isEqualTo(6.8);
        assertThat(captor.getValue().getRecordedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.parcelId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Rechaza un pH inválido sin llamar a parcel-service")
    void shouldRejectInvalidPh() {
        assertThatThrownBy(() -> sensorReadingService.register(validRequest(1L, 15.0)))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Los datos proporcionados no son válidos.");

        verifyNoInteractions(parcelClient);
        verify(sensorReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("No guarda la lectura si la parcela no existe")
    void shouldRejectReadingForMissingParcel() {
        doThrow(new ResourceNotFoundException("No se encontró la parcela con ID 15."))
                .when(parcelClient).ensureParcelExists(15L);

        assertThatThrownBy(() -> sensorReadingService.register(validRequest(15L, 6.8)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la parcela con ID 15.");

        verify(sensorReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("No guarda la lectura si falla la comunicación con parcel-service")
    void shouldPropagateCommunicationFailure() {
        doThrow(new ServiceUnavailableException("No fue posible comunicarse con el servicio de parcelas."))
                .when(parcelClient).ensureParcelExists(1L);

        assertThatThrownBy(() -> sensorReadingService.register(validRequest(1L, 6.8)))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("No fue posible comunicarse con el servicio de parcelas.");

        verify(sensorReadingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devuelve el historial cuando la parcela existe")
    void shouldListReadings() {
        SensorReading stored = storedReading();
        when(sensorReadingRepository.findByParcelIdOrderByRecordedAtDesc(1L)).thenReturn(List.of(stored));

        List<SensorReadingResponse> responses = sensorReadingService.findByParcel(1L);

        verify(parcelClient).ensureParcelExists(1L);
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().airQuality()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("Informa cuando la parcela existe pero todavía no tiene lecturas")
    void shouldFailWhenLatestReadingIsMissing() {
        when(sensorReadingRepository.findFirstByParcelIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorReadingService.findLatest(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No hay lecturas registradas para la parcela con ID 1.");
    }

    private SensorReadingRequest validRequest(long parcelId, double ph) {
        return new SensorReadingRequest(parcelId, 35.5, 22.4, ph, 18.0, 12.0, 20.0, 42.0);
    }

    private SensorReading storedReading() {
        SensorReading reading = new SensorReading();
        reading.setId(10L);
        reading.setParcelId(1L);
        reading.setSoilMoisture(35.5);
        reading.setTemperature(22.4);
        reading.setPh(6.8);
        reading.setNitrogen(18.0);
        reading.setPhosphorus(12.0);
        reading.setPotassium(20.0);
        reading.setAirQuality(42.0);
        reading.setRecordedAt(LocalDateTime.of(2026, 10, 14, 12, 30, 0));
        return reading;
    }
}
