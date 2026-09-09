package cl.equipo.asistencias.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Credenciales incorrectas");
    }
}
