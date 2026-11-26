package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.CitaServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CitaServiceImpl citaService;

    @Test
    void agendaCitaConElVeterinarioSolicitadoYLaMascotaDelCliente() {
        Usuario propietario = Usuario.builder().id(1L).rol(Rol.CLIENTE).build();
        Usuario veterinario = Usuario.builder().id(2L).rol(Rol.VETERINARIO).build();
        Mascota mascota = Mascota.builder().id(3L).propietario(propietario).build();
        LocalDateTime fechaHora = LocalDateTime.of(2027, 1, 15, 10, 0);

        when(mascotaRepository.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita resultado = citaService.agendarCita(3L, 2L, fechaHora, "Control");

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
}