package com.urbangardens.parcel.service.impl;

import com.urbangardens.parcel.dto.LocationDto;
import com.urbangardens.parcel.dto.ParcelRequest;
import com.urbangardens.parcel.dto.ParcelResponse;
import com.urbangardens.parcel.entity.Parcel;
import com.urbangardens.parcel.entity.SoilType;
import com.urbangardens.parcel.exception.InvalidDataException;
import com.urbangardens.parcel.exception.ResourceNotFoundException;
import com.urbangardens.parcel.repository.ParcelRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelServiceImplTest {

    @Mock
    private ParcelRepository parcelRepository;

    private ParcelServiceImpl parcelService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parcelService = new ParcelServiceImpl(parcelRepository, validator);
    }

    @Test
    @DisplayName("Normaliza espacios y conserva acentos antes de guardar")
    void shouldSanitizeTextBeforeSaving() {
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel parcel = invocation.getArgument(0);
            parcel.setId(4L);
            return parcel;
        });

        ParcelRequest request = new ParcelRequest(
                "  Huerto   Comunitario   José  ",
                new LocationDto(28.6329, -106.0691),
                SoilType.SILT,
                "  Ana   Pérez  ");

        ParcelResponse response = parcelService.create(request);

        ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
        verify(parcelRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Huerto Comunitario José");
        assertThat(captor.getValue().getOwnerOrLead()).isEqualTo("Ana Pérez");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.name()).isEqualTo("Huerto Comunitario José");
        assertThat(response.soilType()).isEqualTo(SoilType.SILT);
    }

    @Test
    @DisplayName("Vuelve a validar el texto después de normalizarlo")
    void shouldRejectNameThatBecomesTooShortAfterSanitizing() {
        ParcelRequest request = new ParcelRequest(
                "  aa  ",
                new LocationDto(19.43, -99.13),
                SoilType.SAND,
                "Responsable del huerto");

        assertThatThrownBy(() -> parcelService.create(request))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Los datos proporcionados no son válidos.");
        verify(parcelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza caracteres de control y no guarda la parcela")
    void shouldRejectControlCharacters() {
        ParcelRequest request = new ParcelRequest(
                "Huerto\u0001Norte",
                new LocationDto(19.43, -99.13),
                SoilType.CLAY,
                "Responsable del huerto");

        assertThatThrownBy(() -> parcelService.create(request))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("El texto contiene caracteres de control no permitidos.");
        verify(parcelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la parcela no existe")
    void shouldThrowWhenParcelDoesNotExist() {
        when(parcelRepository.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.findById(15L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la parcela con ID 15.");
    }

    @Test
    @DisplayName("Rechaza un identificador negativo en la capa de servicio")
    void shouldRejectNegativeId() {
        assertThatThrownBy(() -> parcelService.findById(-5L))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("El identificador debe ser un número positivo.");
        verify(parcelRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Devuelve lista vacía cuando el repositorio no tiene parcelas")
    void shouldReturnEmptyList() {
        when(parcelRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(parcelService.findAll()).isEmpty();
    }
}
