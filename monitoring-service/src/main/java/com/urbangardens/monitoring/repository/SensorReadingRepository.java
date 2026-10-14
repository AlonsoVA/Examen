package com.urbangardens.monitoring.repository;

import com.urbangardens.monitoring.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a lecturas. Las consultas se derivan con parámetros enlazados,
 * sin concatenar el identificador de la parcela en SQL.
 */
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByParcelIdOrderByRecordedAtDesc(Long parcelId);

    Optional<SensorReading> findFirstByParcelIdOrderByRecordedAtDesc(Long parcelId);
}
