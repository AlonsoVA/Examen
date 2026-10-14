package com.urbangardens.parcel.repository;

import com.urbangardens.parcel.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a parcelas. Spring Data genera consultas parametrizadas:
 * los datos del usuario no se concatenan en SQL.
 */
public interface ParcelRepository extends JpaRepository<Parcel, Long> {
}
