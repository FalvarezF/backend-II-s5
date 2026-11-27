package com.petsalud.backendpetsalud.exception;

public class SolicitudInvalidaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
