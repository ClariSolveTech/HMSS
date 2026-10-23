package com.healthcare.backend.teleconsult.exception;

public class TeleconsultationException extends RuntimeException {

    public TeleconsultationException(String message) {
        super(message);
    }

    public TeleconsultationException(String message, Throwable cause) {
        super(message, cause);
    }
}
