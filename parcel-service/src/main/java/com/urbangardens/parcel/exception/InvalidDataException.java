package com.urbangardens.parcel.exception;

import com.urbangardens.parcel.dto.FieldErrorDetail;

import java.util.List;

public class InvalidDataException extends RuntimeException {

    private final List<FieldErrorDetail> details;

    public InvalidDataException(String message) {
        this(message, List.of());
    }

    public InvalidDataException(String message, List<FieldErrorDetail> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}
