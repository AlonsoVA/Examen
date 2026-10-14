package com.urbangardens.parcel.controller;

import com.urbangardens.parcel.dto.LocationDto;
import com.urbangardens.parcel.dto.ParcelResponse;
import com.urbangardens.parcel.entity.SoilType;
import com.urbangardens.parcel.exception.GlobalExceptionHandler;
import com.urbangardens.parcel.exception.ResourceNotFoundException;
import com.urbangardens.parcel.service.ParcelService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ParcelController.class)
@Import(GlobalExceptionHandler.class)
class ParcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParcelService parcelService;

    @Test
    @DisplayName("Registra una parcela y responde 201")
    void shouldRegisterParcel() throws Exception {
        when(parcelService.create(any())).thenReturn(sampleParcel());

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validParcelJson()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Huerto Comunitario Norte"))
                .andExpect(jsonPath("$.location.latitude").value(28.6329))
                .andExpect(jsonPath("$.location.longitude").value(-106.0691))
                .andExpect(jsonPath("$.soilType").value("CLAY"))
                .andExpect(jsonPath("$.ownerOrLead").value("Responsable del huerto"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-14T12:30:00"));

        verify(parcelService).create(any());
    }

    @Test
    @DisplayName("Rechaza un nombre vacío con el formato de error personalizado")
    void shouldRejectEmptyName() throws Exception {
        String body = """
                {
                  "name": "",
                  "location": { "latitude": 28.6329, "longitude": -106.0691 },
                  "soilType": "CLAY",
                  "ownerOrLead": "Responsable del huerto"
                }
                """;

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Los datos proporcionados no son válidos."))
                .andExpect(jsonPath("$.path").value("/api/v1/parcels"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[?(@.field == 'name')].message").isNotEmpty());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("Rechaza coordenadas fuera de rango")
    void shouldRejectInvalidCoordinates() throws Exception {
        String body = """
                {
                  "name": "Huerto Comunitario Norte",
                  "location": { "latitude": 128.6329, "longitude": -106.0691 },
                  "soilType": "CLAY",
                  "ownerOrLead": "Responsable del huerto"
                }
                """;

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'location.latitude')].message").isNotEmpty());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("Rechaza un tipo de suelo que no pertenece al enumerado")
    void shouldRejectInvalidSoilType() throws Exception {
        String body = """
                {
                  "name": "Huerto Comunitario Norte",
                  "location": { "latitude": 28.6329, "longitude": -106.0691 },
                  "soilType": "ROCOSO",
                  "ownerOrLead": "Responsable del huerto"
                }
                """;

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATA"))
                .andExpect(jsonPath("$.message").value("El tipo de suelo proporcionado no es válido."))
                .andExpect(jsonPath("$.details[?(@.field == 'soilType')].message").isNotEmpty());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("Devuelve todas las parcelas")
    void shouldListParcels() throws Exception {
        when(parcelService.findAll()).thenReturn(List.of(sampleParcel()));

        mockMvc.perform(get("/api/v1/parcels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Huerto Comunitario Norte"));
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay parcelas")
    void shouldReturnEmptyList() throws Exception {
        when(parcelService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parcels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Consulta una parcela existente")
    void shouldReturnParcelById() throws Exception {
        when(parcelService.findById(1L)).thenReturn(sampleParcel());

        mockMvc.perform(get("/api/v1/parcels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.soilType").value("CLAY"));
    }

    @Test
    @DisplayName("Responde 404 cuando la parcela no existe")
    void shouldReturnNotFoundWhenParcelDoesNotExist() throws Exception {
        when(parcelService.findById(15L))
                .thenThrow(new ResourceNotFoundException("No se encontró la parcela con ID 15."));

        mockMvc.perform(get("/api/v1/parcels/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No se encontró la parcela con ID 15."))
                .andExpect(jsonPath("$.path").value("/api/v1/parcels/15"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("Rechaza un identificador negativo")
    void shouldRejectNegativeId() throws Exception {
        mockMvc.perform(get("/api/v1/parcels/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Los datos proporcionados no son válidos."))
                .andExpect(jsonPath("$.details[?(@.field == 'id')].message").isNotEmpty());

        verify(parcelService, never()).findById(any());
    }

    @Test
    @DisplayName("Rechaza un identificador que no es numérico")
    void shouldRejectNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/parcels/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'id')].message").isNotEmpty());

        verifyNoInteractions(parcelService);
    }

    private ParcelResponse sampleParcel() {
        return new ParcelResponse(
                1L,
                "Huerto Comunitario Norte",
                new LocationDto(28.6329, -106.0691),
                SoilType.CLAY,
                "Responsable del huerto",
                LocalDateTime.of(2026, 10, 14, 12, 30, 0));
    }

    private String validParcelJson() {
        return """
                {
                  "name": "Huerto Comunitario Norte",
                  "location": { "latitude": 28.6329, "longitude": -106.0691 },
                  "soilType": "CLAY",
                  "ownerOrLead": "Responsable del huerto"
                }
                """;
    }
}
