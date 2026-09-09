package cl.equipo.asistencias.service;

import cl.equipo.asistencias.dto.LoginRequest;
import cl.equipo.asistencias.dto.TokenResponse;
import cl.equipo.asistencias.exception.CredencialesInvalidasException;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.UsuarioRepository;
import cl.equipo.asistencias.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())) {
            throw new CredencialesInvalidasException();
        }
        return new TokenResponse(jwtService.generar(usuario), "Bearer");
    }
}
