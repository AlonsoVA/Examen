package com.urbangardens.monitoring.controller;

import com.urbangardens.monitoring.dto.SensorReadingResponse;
import com.urbangardens.monitoring.exception.GlobalExceptionHandler;
import com.urbangardens.monitoring.exception.ResourceNotFoundException;
import com.urbangardens.monitoring.exception.ServiceUnavailableException;
import com.urbangardens.monitoring.service.SensorReadingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SensorReadingController.class)
@Import(GlobalExceptionHandler.class)
class SensorReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorReadingService sensorReadingService;

    @Test
    @DisplayName("Registra una lectura y responde 201")
    void shouldRegisterReading() throws Exception {
        when(sensorReadingService.register(any())).thenReturn(sampleReading());

        mockMvc.perform(post("/api/v1/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReadingJson(1L, 6.8)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.parcelId").value(1))
                .andExpect(jsonPath("$.ph").value(6.8))
                .andExpect(jsonPath("$.recordedAt").value("2026-10-14T12:30:00"));
    }

    @Test
    @DisplayName("Rechaza un pH fuera de rango")
    void shouldRejectInvalidPh() throws Exception {
        mockMvc.perform(post("/api/v1/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReadingJson(1L, 15.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Los datos proporcionados no son válidos."))
                .andExpect(jsonPath("$.path").value("/api/v1/readings"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.details[?(@.field == 'ph')].message").isNotEmpty());

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @DisplayName("Responde 404 si la parcela de la lectura no existe")
    void shouldReturnNotFoundWhenParcelDoesNotExist() throws Exception {
        when(sensorReadingService.register(any()))
                .thenThrow(new ResourceNotFoundException("No se encontró la parcela con ID 15."));

        mockMvc.perform(post("/api/v1/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReadingJson(15L, 6.8)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No se encontró la parcela con ID 15."))
                .andExpect(jsonPath("$.path").value("/api/v1/readings"));
    }

    @Test
    @DisplayName("Responde 503 cuando parcel-service no está disponible")
    void shouldReturnServiceUnavailableWhenCommunicationFails() throws Exception {
        when(sensorReadingService.register(any()))
                .thenThrow(new ServiceUnavailableException(
                        "No fue posible comunicarse con el servicio de parcelas."));

        mockMvc.perform(post("/api/v1/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReadingJson(1L, 6.8)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("No fue posible comunicarse con el servicio de parcelas."))
                .andExpect(jsonPath("$.path").value("/api/v1/readings"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("Consulta el historial de lecturas de una parcela")
    void shouldListReadingsByParcel() throws Exception {
        when(sensorReadingService.findByParcel(1L)).thenReturn(List.of(sampleReading()));

        mockMvc.perform(get("/api/v1/readings/parcel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].parcelId").value(1));
    }

    @Test
    @DisplayName("Consulta la última lectura de una parcela")
    void shouldReturnLatestReading() throws Exception {
        when(sensorReadingService.findLatest(1L)).thenReturn(sampleReading());

        mockMvc.perform(get("/api/v1/readings/parcel/1/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.temperature").value(22.4));
    }

    private SensorReadingResponse sampleReading() {
        return new SensorReadingResponse(
                10L, 1L, 35.5, 22.4, 6.8, 18.0, 12.0, 20.0, 42.0,
                LocalDateTime.of(2026, 10, 14, 12, 30, 0));
    }

    private String validReadingJson(long parcelId, double ph) {
        return """
                {
                  "parcelId": %d,
                  "soilMoisture": 35.5,
                  "temperature": 22.4,
                  "ph": %s,
                  "nitrogen": 18.0,
                  "phosphorus": 12.0,
                  "potassium": 20.0,
                  "airQuality": 42.0
                }
                """.formatted(parcelId, ph);
    }
}
