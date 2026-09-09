package cl.equipo.asistencias.controller;

import cl.equipo.asistencias.dto.LoginRequest;
import cl.equipo.asistencias.dto.TokenResponse;
import cl.equipo.asistencias.dto.UsuarioResponse;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login: retorna un JWT válido por 8 horas")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Datos del usuario autenticado (leídos del token)")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Usuario usuario) {
        return UsuarioResponse.desde(usuario);
    }
}
