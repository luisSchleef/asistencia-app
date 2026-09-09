package cl.equipo.asistencias.dto;

import cl.equipo.asistencias.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioActualizarRequest(
        @NotBlank String nombre,
        @NotBlank @Email String correo,
        @Size(min = 6, message = "mínimo 6 caracteres") String contrasena,
        @NotNull Rol rol
) {}
