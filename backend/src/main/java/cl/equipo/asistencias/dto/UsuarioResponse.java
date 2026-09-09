package cl.equipo.asistencias.dto;

import cl.equipo.asistencias.model.Rol;
import cl.equipo.asistencias.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String correo, Rol rol) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol());
    }
}
