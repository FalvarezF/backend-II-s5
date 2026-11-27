package com.petsalud.backendpetsalud.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
