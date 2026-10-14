package com.urbangardens.monitoring.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
