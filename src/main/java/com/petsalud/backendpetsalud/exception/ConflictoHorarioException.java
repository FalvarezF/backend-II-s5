package com.petsalud.backendpetsalud.exception;

public class ConflictoHorarioException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictoHorarioException(String message) {
        super(message);
    }
}
