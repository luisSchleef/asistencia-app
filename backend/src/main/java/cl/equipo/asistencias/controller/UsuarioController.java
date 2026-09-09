package cl.equipo.asistencias.controller;

import cl.equipo.asistencias.dto.UsuarioActualizarRequest;
import cl.equipo.asistencias.dto.UsuarioRequest;
import cl.equipo.asistencias.dto.UsuarioResponse;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Crear usuario")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @Operation(summary = "Actualizar usuario (contraseña vacía = sin cambio)")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @Operation(summary = "Eliminar usuario (cascade elimina sus asistencias)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        usuarioService.eliminar(id, actor.getId());
        return ResponseEntity.noContent().build();
    }
}
