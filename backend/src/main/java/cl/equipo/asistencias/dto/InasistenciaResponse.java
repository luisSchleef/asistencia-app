package cl.equipo.asistencias.dto;

import java.time.LocalDate;

public record InasistenciaResponse(
        Long usuarioId,
        String usuarioNombre,
        LocalDate fecha
) {}
