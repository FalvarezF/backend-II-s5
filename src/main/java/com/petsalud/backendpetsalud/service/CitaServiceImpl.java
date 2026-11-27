package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.exception.ConflictoHorarioException;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.SolicitudInvalidaException;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CitaServiceImpl implements CitaService {

    private static final long DURACION_CITA_MINUTOS = 30;
    private static final String ESTADO_AGENDADA = "Agendada";
    private static final Set<String> ESTADOS_VALIDOS = Set.of("Agendada", "Cancelada", "Completada");

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Clock clock;

    @Override
    public Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo) {
        if (fechaHora.isAfter(LocalDateTime.now(clock).plusMonths(6))) {
            throw new SolicitudInvalidaException("La fecha de la cita no puede superar los 6 meses desde hoy");
        }

        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));

        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));

        boolean horarioOcupado = citaRepository
                .existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                        veterinarioId,
                        ESTADO_AGENDADA,
                        fechaHora.minusMinutes(DURACION_CITA_MINUTOS),
                        fechaHora.plusMinutes(DURACION_CITA_MINUTOS)
                );
        if (horarioOcupado) {
            throw new ConflictoHorarioException("El veterinario no tiene disponibilidad en ese horario");
        }

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo(motivo)
                .estado("Agendada")
                .build();

        return citaRepository.save(cita);
    }

    @Override
    public Optional<Cita> obtenerCitaPorId(Long id) {
        return citaRepository.findById(id);
    }

    @Override
    public Cita actualizarEstadoCita(Long id, String nuevoEstado) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (nuevoEstado == null || !ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new SolicitudInvalidaException("Estado de cita no valido");
        }

        cita.setEstado(nuevoEstado);
        return citaRepository.save(cita);
    }

    @Override
    public void cancelarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        citaRepository.delete(cita);
    }

    @Override
    public List<Cita> listarCitasPorMascota(Long mascotaId) {
        mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
        return citaRepository.findByMascotaId(mascotaId);
    }

    @Override
    public List<Cita> listarCitasPorVeterinario(Long veterinarioId) {
        usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
        return citaRepository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        if (inicio.isAfter(fin)) {
            throw new SolicitudInvalidaException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (inicio.isBefore(ahora)) {
            throw new SolicitudInvalidaException("La fecha de inicio no puede estar en el pasado");
        }
        if (fin.isAfter(ahora.plusMonths(6))) {
            throw new SolicitudInvalidaException("La fecha de fin no puede superar los 6 meses desde hoy");
        }

        return citaRepository.findByFechaHoraBetween(inicio, fin);
    }
}

