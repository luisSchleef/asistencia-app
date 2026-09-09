package cl.equipo.asistencias.dto;

import cl.equipo.asistencias.model.Asistencia;

import java.time.LocalDate;
import java.time.LocalTime;

public record AsistenciaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String tipo,
        LocalDate fecha,
        LocalTime hora
) {
    public static AsistenciaResponse desde(Asistencia a) {
        return new AsistenciaResponse(
                a.getId(),
                a.getUsuario().getId(),
                a.getUsuario().getNombre(),
                a.getTipo().name(),
                a.getFecha(),
                a.getHora());
    }
}
