package cl.equipo.asistencias.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String error, String mensaje) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Datos inválidos",
                "campos", campos));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> sinPermisos(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, new AccessDeniedException("No tiene permisos para esta operación"));
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(CorreoDuplicadoException ex) {
        return responder(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> noPermitida(OperacionNoPermitidaException ex) {
        return responder(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonMalformado(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, new IllegalArgumentException("JSON inválido"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {

        return ResponseEntity.internalServerError().body(
                new ErrorResponse(500, "Error interno", "Ocurrió un error inesperado"));
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }
}
