package com.urbangardens.monitoring.client;

import com.urbangardens.monitoring.exception.ResourceNotFoundException;
import com.urbangardens.monitoring.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ParcelClient {

    private static final Logger log = LoggerFactory.getLogger(ParcelClient.class);

    private final RestClient parcelRestClient;

    public ParcelClient(RestClient parcelRestClient) {
        this.parcelRestClient = parcelRestClient;
    }

    public void ensureParcelExists(Long parcelId) {
        try {
            parcelRestClient.get()
                    .uri("/api/v1/parcels/{id}", parcelId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("No se encontró la parcela con ID " + parcelId + ".");
            }
            log.warn("Fallo al consultar parcel-service para la parcela {}: HTTP {}", parcelId, ex.getStatusCode().value());
            throw new ServiceUnavailableException("No fue posible comunicarse con el servicio de parcelas.");
        } catch (RestClientException ex) {
            log.warn("Fallo al consultar parcel-service para la parcela {}: {}", parcelId, ex.getMessage());
            throw new ServiceUnavailableException("No fue posible comunicarse con el servicio de parcelas.");
        }
    }
}
