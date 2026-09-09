package cl.equipo.asistencias.service;

import cl.equipo.asistencias.dto.UsuarioActualizarRequest;
import cl.equipo.asistencias.dto.UsuarioRequest;
import cl.equipo.asistencias.dto.UsuarioResponse;
import cl.equipo.asistencias.exception.CorreoDuplicadoException;
import cl.equipo.asistencias.exception.OperacionNoPermitidaException;
import cl.equipo.asistencias.exception.RecursoNoEncontradoException;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new CorreoDuplicadoException();
        }
        Usuario usuario = new Usuario(
                request.nombre(),
                request.correo(),
                passwordEncoder.encode(request.contrasena()),
                request.rol());
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest request) {
        Usuario usuario = porId(id);

        if (!usuario.getCorreo().equals(request.correo())
                && usuarioRepository.existsByCorreo(request.correo())) {
            throw new CorreoDuplicadoException();
        }
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setRol(request.rol());
        if (request.contrasena() != null && !request.contrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        }
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public void eliminar(Long id, Long actorId) {
        if (id.equals(actorId)) {

            throw new OperacionNoPermitidaException("No puede eliminar su propio usuario");
        }
        Usuario usuario = porId(id);

        usuarioRepository.delete(usuario);
    }

    private Usuario porId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no existe"));
    }
}
