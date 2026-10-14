package com.urbangardens.monitoring.client;

import com.urbangardens.monitoring.exception.ResourceNotFoundException;
import com.urbangardens.monitoring.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class ParcelClientTest {

    private MockRestServiceServer server;
    private ParcelClient parcelClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://parcel-service");
        server = MockRestServiceServer.bindTo(builder).build();
        parcelClient = new ParcelClient(builder.build());
    }

    @Test
    @DisplayName("Acepta la parcela cuando parcel-service responde 200")
    void shouldAcceptExistingParcel() {
        server.expect(requestTo("http://parcel-service/api/v1/parcels/1"))
                .andExpect(method(GET))
                .andRespond(withSuccess());

        assertThatCode(() -> parcelClient.ensureParcelExists(1L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("Traduce un 404 del servicio de parcelas a recurso no encontrado")
    void shouldThrowWhenParcelDoesNotExist() {
        server.expect(requestTo("http://parcel-service/api/v1/parcels/15"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> parcelClient.ensureParcelExists(15L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la parcela con ID 15.");
        server.verify();
    }

    @Test
    @DisplayName("Traduce un error HTTP del servicio de parcelas a servicio no disponible")
    void shouldThrowWhenParcelServiceReturnsServerError() {
        server.expect(requestTo("http://parcel-service/api/v1/parcels/1"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> parcelClient.ensureParcelExists(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("No fue posible comunicarse con el servicio de parcelas.");
        server.verify();
    }

    @Test
    @DisplayName("Traduce una falla de conexión a servicio no disponible")
    void shouldThrowWhenConnectionFails() {
        server.expect(requestTo("http://parcel-service/api/v1/parcels/1"))
                .andExpect(method(GET))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> parcelClient.ensureParcelExists(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("No fue posible comunicarse con el servicio de parcelas.");
        server.verify();
    }
}
