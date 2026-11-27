package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.exception.ConflictoHorarioException;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.exception.SolicitudInvalidaException;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 26, 21, 0);

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private CitaServiceImpl citaService;

    @BeforeEach
    void fijaElRelojDelServicio() {
        lenient().when(clock.instant()).thenReturn(AHORA.atZone(ZONA).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZONA);
    }

    @Test
    void agendaCitaExitosaConMascotaYVeterinarioExistentes() {
        Usuario propietario = Usuario.builder().id(1L).rol(Rol.CLIENTE).build();
        Usuario veterinario = Usuario.builder().id(2L).rol(Rol.VETERINARIO).build();
        Mascota mascota = Mascota.builder().id(3L).propietario(propietario).build();
        LocalDateTime fechaHora = LocalDateTime.of(2027, 1, 15, 10, 0);

        when(mascotaRepository.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                2L,
                "Agendada",
                fechaHora.minusMinutes(30),
                fechaHora.plusMinutes(30)
        )).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita resultado = citaService.agendarCita(3L, 2L, fechaHora, "Control");

        verify(mascotaRepository).findById(3L);
        verify(usuarioRepository).findById(2L);
        verify(citaRepository).existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                2L,
                "Agendada",
                fechaHora.minusMinutes(30),
                fechaHora.plusMinutes(30)
        );

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(captor.capture());
        Cita citaGuardada = captor.getValue();

        assertSame(citaGuardada, resultado);
        assertEquals("Agendada", citaGuardada.getEstado());
        assertEquals(fechaHora, citaGuardada.getFechaHora());
        assertEquals("Control", citaGuardada.getMotivo());
        assertSame(veterinario, citaGuardada.getVeterinario());
        assertSame(mascota, citaGuardada.getMascota());
        assertSame(propietario, citaGuardada.getMascota().getPropietario());
    }

    @Test
    void agendaCitaExactamenteEnElLimiteDeSeisMeses() {
        Usuario veterinario = Usuario.builder().id(2L).rol(Rol.VETERINARIO).build();
        Mascota mascota = Mascota.builder().id(3L).build();
        LocalDateTime fechaHora = AHORA.plusMonths(6);

        when(mascotaRepository.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                2L,
                "Agendada",
                fechaHora.minusMinutes(30),
                fechaHora.plusMinutes(30)
        )).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita resultado = citaService.agendarCita(3L, 2L, fechaHora, "Control");

        assertEquals(fechaHora, resultado.getFechaHora());
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    void rechazaAgendarCitaPosteriorAlLimiteDeSeisMeses() {
        LocalDateTime fechaHora = AHORA.plusMonths(6).plusNanos(1);

        SolicitudInvalidaException excepcion = assertThrows(
                SolicitudInvalidaException.class,
                () -> citaService.agendarCita(3L, 2L, fechaHora, "Control")
        );

        assertEquals("La fecha de la cita no puede superar los 6 meses desde hoy", excepcion.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository, citaRepository);
    }

    @Test
    void noAgendaCitaCuandoElHorarioSeSolapaConOtraCitaAgendada() {
        Usuario veterinario = Usuario.builder().id(2L).rol(Rol.VETERINARIO).build();
        Mascota mascota = Mascota.builder().id(3L).build();
        LocalDateTime fechaHora = LocalDateTime.of(2027, 1, 15, 10, 0);

        when(mascotaRepository.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                2L,
                "Agendada",
                fechaHora.minusMinutes(30),
                fechaHora.plusMinutes(30)
        )).thenReturn(true);

        ConflictoHorarioException excepcion = assertThrows(
                ConflictoHorarioException.class,
                () -> citaService.agendarCita(3L, 2L, fechaHora, "Control")
        );

        assertEquals("El veterinario no tiene disponibilidad en ese horario", excepcion.getMessage());
        verify(citaRepository).existsByVeterinarioIdAndEstadoAndFechaHoraGreaterThanAndFechaHoraLessThan(
                2L,
                "Agendada",
                fechaHora.minusMinutes(30),
                fechaHora.plusMinutes(30)
        );
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void noAgendaCitaCuandoLaMascotaNoExiste() {
        when(mascotaRepository.findById(3L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.agendarCita(
                        3L,
                        2L,
                        LocalDateTime.of(2027, 1, 15, 10, 0),
                        "Control"
                )
        );

        assertEquals("Mascota no encontrada", excepcion.getMessage());
        verify(mascotaRepository).findById(3L);
        verifyNoInteractions(usuarioRepository, citaRepository);
    }

    @Test
    void noAgendaCitaCuandoElVeterinarioNoExiste() {
        Mascota mascota = Mascota.builder().id(3L).build();
        when(mascotaRepository.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.agendarCita(
                        3L,
                        2L,
                        LocalDateTime.of(2027, 1, 15, 10, 0),
                        "Control"
                )
        );

        assertEquals("Veterinario no encontrado", excepcion.getMessage());
        verify(mascotaRepository).findById(3L);
        verify(usuarioRepository).findById(2L);
        verifyNoInteractions(citaRepository);
    }

    @Test
    void obtieneCitaPorIdCuandoExiste() {
        Cita cita = Cita.builder()
                .id(10L)
                .estado("Agendada")
                .build();

        when(citaRepository.findById(10L)).thenReturn(Optional.of(cita));

        Optional<Cita> resultado = citaService.obtenerCitaPorId(10L);

        assertTrue(resultado.isPresent());
        assertSame(cita, resultado.orElseThrow());
        verify(citaRepository).findById(10L);
    }

    @Test
    void devuelveOptionalVacioCuandoNoExisteLaCita() {
        when(citaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Cita> resultado = citaService.obtenerCitaPorId(99L);

        assertFalse(resultado.isPresent());
        verify(citaRepository).findById(99L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Agendada", "Cancelada", "Completada"})
    void actualizaEstadoDeCitaExistenteConEstadoPermitido(String nuevoEstado) {
        Cita cita = Cita.builder()
                .id(5L)
                .estado("Agendada")
                .build();

        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita resultado = citaService.actualizarEstadoCita(5L, nuevoEstado);

        assertEquals(nuevoEstado, resultado.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void actualizarEstadoLanzaExcepcionCuandoLaCitaNoExiste() {
        when(citaRepository.findById(5L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.actualizarEstadoCita(5L, "Completada")
        );

        assertEquals("Cita no encontrada", excepcion.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" ", "Otro"})
    void rechazaEstadoNuloVacioODesconocido(String nuevoEstado) {
        Cita cita = Cita.builder()
                .id(5L)
                .estado("Agendada")
                .build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));

        SolicitudInvalidaException excepcion = assertThrows(
                SolicitudInvalidaException.class,
                () -> citaService.actualizarEstadoCita(5L, nuevoEstado)
        );

        assertEquals("Estado de cita no valido", excepcion.getMessage());
        assertEquals("Agendada", cita.getEstado());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void cancelarCitaExistenteEliminaLaCita() {
        Long citaId = 7L;
        Cita cita = Cita.builder()
                .id(citaId)
                .estado("Agendada")
                .build();
        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        citaService.cancelarCita(citaId);

        verify(citaRepository).findById(citaId);
        verify(citaRepository).delete(cita);
    }

    @Test
    void cancelarCitaInexistenteLanzaExcepcion() {
        Long citaId = 99L;
        when(citaRepository.findById(citaId)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.cancelarCita(citaId)
        );

        assertEquals("Cita no encontrada", excepcion.getMessage());
        verify(citaRepository).findById(citaId);
        verify(citaRepository, never()).delete(any(Cita.class));
    }

    @Test
    void listaCitasCuandoLaMascotaTieneCitas() {
        Long mascotaId = 3L;
        Mascota mascota = Mascota.builder().id(mascotaId).build();
        List<Cita> citasEsperadas = List.of(
                Cita.builder().id(10L).build(),
                Cita.builder().id(11L).build()
        );

        when(mascotaRepository.findById(mascotaId)).thenReturn(Optional.of(mascota));
        when(citaRepository.findByMascotaId(mascotaId)).thenReturn(citasEsperadas);

        List<Cita> resultado = citaService.listarCitasPorMascota(mascotaId);

        assertSame(citasEsperadas, resultado);
        verify(mascotaRepository).findById(mascotaId);
        verify(citaRepository).findByMascotaId(mascotaId);
    }

    @Test
    void devuelveListaVaciaCuandoLaMascotaExisteSinCitas() {
        Long mascotaId = 3L;
        Mascota mascota = Mascota.builder().id(mascotaId).build();
        when(mascotaRepository.findById(mascotaId)).thenReturn(Optional.of(mascota));
        when(citaRepository.findByMascotaId(mascotaId)).thenReturn(List.of());

        List<Cita> resultado = citaService.listarCitasPorMascota(mascotaId);

        assertTrue(resultado.isEmpty());
        verify(mascotaRepository).findById(mascotaId);
        verify(citaRepository).findByMascotaId(mascotaId);
    }

    @Test
    void lanzaExcepcionCuandoLaMascotaNoExisteAlListarCitas() {
        Long mascotaId = 99L;
        when(mascotaRepository.findById(mascotaId)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.listarCitasPorMascota(mascotaId)
        );

        assertEquals("Mascota no encontrada", excepcion.getMessage());
        verify(mascotaRepository).findById(mascotaId);
        verifyNoInteractions(citaRepository);
    }

    @Test
    void listaCitasCuandoElVeterinarioTieneCitas() {
        Long veterinarioId = 4L;
        Usuario veterinario = Usuario.builder()
                .id(veterinarioId)
                .rol(Rol.VETERINARIO)
                .build();
        List<Cita> citasEsperadas = List.of(
                Cita.builder().id(20L).build(),
                Cita.builder().id(21L).build()
        );

        when(usuarioRepository.findById(veterinarioId)).thenReturn(Optional.of(veterinario));
        when(citaRepository.findByVeterinarioId(veterinarioId)).thenReturn(citasEsperadas);

        List<Cita> resultado = citaService.listarCitasPorVeterinario(veterinarioId);

        assertSame(citasEsperadas, resultado);
        verify(usuarioRepository).findById(veterinarioId);
        verify(citaRepository).findByVeterinarioId(veterinarioId);
    }

    @Test
    void devuelveListaVaciaCuandoElVeterinarioExisteSinCitas() {
        Long veterinarioId = 4L;
        Usuario veterinario = Usuario.builder()
                .id(veterinarioId)
                .rol(Rol.VETERINARIO)
                .build();
        when(usuarioRepository.findById(veterinarioId)).thenReturn(Optional.of(veterinario));
        when(citaRepository.findByVeterinarioId(veterinarioId)).thenReturn(List.of());

        List<Cita> resultado = citaService.listarCitasPorVeterinario(veterinarioId);

        assertTrue(resultado.isEmpty());
        verify(usuarioRepository).findById(veterinarioId);
        verify(citaRepository).findByVeterinarioId(veterinarioId);
    }

    @Test
    void lanzaExcepcionCuandoElVeterinarioNoExisteAlListarCitas() {
        Long veterinarioId = 99L;
        when(usuarioRepository.findById(veterinarioId)).thenReturn(Optional.empty());

        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> citaService.listarCitasPorVeterinario(veterinarioId)
        );

        assertEquals("Veterinario no encontrado", excepcion.getMessage());
        verify(usuarioRepository).findById(veterinarioId);
        verifyNoInteractions(citaRepository);
    }

    @Test
    void listaCitasDentroDelPeriodo() {
        LocalDateTime inicio = AHORA.plusMinutes(1);
        LocalDateTime fin = inicio.plusDays(2);
        List<Cita> citasEsperadas = List.of(
                Cita.builder().fechaHora(inicio.plusHours(1)).build(),
                Cita.builder().fechaHora(inicio.plusHours(2)).build()
        );
        when(citaRepository.findByFechaHoraBetween(inicio, fin)).thenReturn(citasEsperadas);

        List<Cita> resultado = citaService.listarCitasEntreFechas(inicio, fin);

        assertSame(citasEsperadas, resultado);
        verify(citaRepository).findByFechaHoraBetween(inicio, fin);
    }

    @Test
    void devuelveListaVaciaCuandoNoHayCitasEnElPeriodo() {
        LocalDateTime inicio = AHORA.plusMinutes(1);
        LocalDateTime fin = inicio.plusDays(2);
        when(citaRepository.findByFechaHoraBetween(inicio, fin)).thenReturn(List.of());

        List<Cita> resultado = citaService.listarCitasEntreFechas(inicio, fin);

        assertTrue(resultado.isEmpty());
        verify(citaRepository).findByFechaHoraBetween(inicio, fin);
    }

    @Test
    void rechazaFechaDeInicioEnElPasado() {
        LocalDateTime inicio = AHORA.minusMinutes(1);
        LocalDateTime fin = AHORA.plusHours(1);

        SolicitudInvalidaException excepcion = assertThrows(
                SolicitudInvalidaException.class,
                () -> citaService.listarCitasEntreFechas(inicio, fin)
        );

        assertEquals("La fecha de inicio no puede estar en el pasado", excepcion.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    void rechazaFechaDeFinPosteriorASeisMeses() {
        LocalDateTime inicio = AHORA.plusMinutes(1);
        LocalDateTime fin = AHORA.plusMonths(6).plusDays(1);

        SolicitudInvalidaException excepcion = assertThrows(
                SolicitudInvalidaException.class,
                () -> citaService.listarCitasEntreFechas(inicio, fin)
        );

        assertEquals("La fecha de fin no puede superar los 6 meses desde hoy", excepcion.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    void rechazaPeriodoCuandoLaFechaDeInicioEsPosteriorALaFechaDeFin() {
        LocalDateTime inicio = AHORA.plusHours(2);
        LocalDateTime fin = AHORA.plusHours(1);

        SolicitudInvalidaException excepcion = assertThrows(
                SolicitudInvalidaException.class,
                () -> citaService.listarCitasEntreFechas(inicio, fin)
        );

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de fin", excepcion.getMessage());
        verifyNoInteractions(citaRepository);
    }
}