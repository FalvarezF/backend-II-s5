package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.exception.ConflictoHorarioException;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.format.DateTimeParseException;

@RestControllerAdvice(assignableTypes = CitaController.class)
public class CitaExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                excepcion.getMessage(),
                request
        );
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ProblemDetail> manejarSolicitudInvalida(
            SolicitudInvalidaException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.BAD_REQUEST,
                "Solicitud invalida",
                excepcion.getMessage(),
                request
        );
    }

    @ExceptionHandler(ConflictoHorarioException.class)
    public ResponseEntity<ProblemDetail> manejarConflictoHorario(
            ConflictoHorarioException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.CONFLICT,
                "Conflicto de horario",
                excepcion.getMessage(),
                request
        );
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ProblemDetail> manejarFechaInvalida(
            DateTimeParseException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.BAD_REQUEST,
                "Fecha u hora invalida",
                "Usa el formato ISO-8601, por ejemplo 2026-12-01T10:00:00.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> manejarTipoDeParametroInvalido(
            MethodArgumentTypeMismatchException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.BAD_REQUEST,
                "Parametro invalido",
                "El parametro '" + excepcion.getName() + "' tiene un formato invalido.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> manejarParametroFaltante(
            MissingServletRequestParameterException excepcion,
            HttpServletRequest request
    ) {
        return crearProblema(
                HttpStatus.BAD_REQUEST,
                "Parametro requerido",
                "Falta el parametro '" + excepcion.getParameterName() + "'.",
                request
        );
    }

    private ResponseEntity<ProblemDetail> crearProblema(
            HttpStatus status,
            String titulo,
            String detalle,
            HttpServletRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problema);
    }
}
