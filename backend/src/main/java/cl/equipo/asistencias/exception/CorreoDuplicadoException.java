package cl.equipo.asistencias.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException() {
        super("El correo ya está registrado");
    }
}
