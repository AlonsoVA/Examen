package com.urbangardens.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Lectura ambiental de una parcela.
 * Unidades: humedad del suelo en porcentaje (0 a 100); temperatura en grados Celsius
 * (rango aceptado de -40 a 60); pH en la escala de 0 a 14; nitrógeno, fósforo y potasio
 * en mg/kg de suelo; calidad del aire como índice AQI, no negativo.
 * parcelId referencia una parcela de parcel-service. No hay relación JPA entre bases.
 */
@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_readings_parcel_id", columnList = "parcel_id")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parcel_id", nullable = false)
    private Long parcelId;

    @Column(name = "soil_moisture", nullable = false)
    private Double soilMoisture;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "ph", nullable = false)
    private Double ph;

    @Column(name = "nitrogen", nullable = false)
    private Double nitrogen;

    @Column(name = "phosphorus", nullable = false)
    private Double phosphorus;

    @Column(name = "potassium", nullable = false)
    private Double potassium;

    @Column(name = "air_quality", nullable = false)
    private Double airQuality;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParcelId() {
        return parcelId;
    }

    public void setParcelId(Long parcelId) {
        this.parcelId = parcelId;
    }

    public Double getSoilMoisture() {
        return soilMoisture;
    }

    public void setSoilMoisture(Double soilMoisture) {
        this.soilMoisture = soilMoisture;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getNitrogen() {
        return nitrogen;
    }

    public void setNitrogen(Double nitrogen) {
        this.nitrogen = nitrogen;
    }

    public Double getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(Double phosphorus) {
        this.phosphorus = phosphorus;
    }

    public Double getPotassium() {
        return potassium;
    }

    public void setPotassium(Double potassium) {
        this.potassium = potassium;
    }

    public Double getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(Double airQuality) {
        this.airQuality = airQuality;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @PrePersist
    void assignRecordedAt() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        }
    }
}
