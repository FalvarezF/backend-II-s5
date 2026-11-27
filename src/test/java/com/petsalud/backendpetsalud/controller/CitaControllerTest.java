package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.exception.ConflictoHorarioException;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.SolicitudInvalidaException;
import com.petsalud.backendpetsalud.service.CitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CitaControllerTest {

    @Mock
    private CitaService citaService;

    @InjectMocks
    private CitaController citaController;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(citaController)
                .setControllerAdvice(new CitaExceptionHandler())
                .build();
    }

    @Test
    void agendarCitaDevuelveCreatedYLocation() throws Exception {
        LocalDateTime fechaHora = LocalDateTime.of(2027, 1, 15, 10, 0);
        Cita cita = Cita.builder()
                .id(12L)
                .fechaHora(fechaHora)
                .estado("Agendada")
                .motivo("Control")
                .build();
        when(citaService.agendarCita(1L, 2L, fechaHora, "Control")).thenReturn(cita);

        mockMvc.perform(post("/api/citas")
                        .param("mascotaId", "1")
                        .param("veterinarioId", "2")
                        .param("fechaHora", fechaHora.toString())
                        .param("motivo", "Control"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/citas/12")));
    }

    @Test
    void obtenerCitaInexistenteDevuelve404ConProblemDetail() throws Exception {
        when(citaService.obtenerCitaPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/citas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cita no encontrada"));
    }

    @Test
    void estadoInvalidoDevuelve400ConProblemDetail() throws Exception {
        when(citaService.actualizarEstadoCita(12L, "En curso"))
                .thenThrow(new SolicitudInvalidaException("Estado de cita no valido"));

        mockMvc.perform(put("/api/citas/12/estado").param("estado", "En curso"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Estado de cita no valido"));
    }

    @Test
    void horarioOcupadoDevuelve409ConProblemDetail() throws Exception {
        LocalDateTime fechaHora = LocalDateTime.of(2027, 1, 15, 10, 0);
        when(citaService.agendarCita(1L, 2L, fechaHora, "Control"))
                .thenThrow(new ConflictoHorarioException(
                        "El veterinario no tiene disponibilidad en ese horario"
                ));

        mockMvc.perform(post("/api/citas")
                        .param("mascotaId", "1")
                        .param("veterinarioId", "2")
                        .param("fechaHora", fechaHora.toString())
                        .param("motivo", "Control"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("El veterinario no tiene disponibilidad en ese horario"));
    }

    @Test
    void fechaMalFormadaDevuelve400ConProblemDetail() throws Exception {
        mockMvc.perform(get("/api/citas/entre-fechas")
                        .param("inicio", "fecha-invalida")
                        .param("fin", "2026-12-01T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Usa el formato ISO-8601, por ejemplo 2026-12-01T10:00:00."));
    }

    @Test
    void listarSinResultadosDevuelve200YListaVacia() throws Exception {
        when(citaService.listarCitasPorMascota(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/citas/mascota/3"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void cancelarCitaDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/citas/12"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void veterinarioInexistenteDevuelve404ConProblemDetail() throws Exception {
        when(citaService.listarCitasPorVeterinario(99L))
                .thenThrow(new RecursoNoEncontradoException("Veterinario no encontrado"));

        mockMvc.perform(get("/api/citas/veterinario/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Veterinario no encontrado"));
    }

    @Test
    void idConFormatoInvalidoDevuelve400ConProblemDetail() throws Exception {
        mockMvc.perform(get("/api/citas/no-es-un-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("id")));
    }
}
