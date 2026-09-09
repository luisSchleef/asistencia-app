package cl.equipo.asistencias.service;

import cl.equipo.asistencias.dto.AsistenciaResponse;
import cl.equipo.asistencias.exception.OperacionNoPermitidaException;
import cl.equipo.asistencias.model.Asistencia;
import cl.equipo.asistencias.model.TipoAsistencia;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.AsistenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository) {
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional
    public AsistenciaResponse marcar(Usuario usuario, TipoAsistencia tipo) {
        ZoneId zona = ZoneId.of("America/Santiago");
        LocalDate hoy = LocalDate.now(zona);
        if (asistenciaRepository.existsByUsuarioAndFechaAndTipo(usuario, hoy, tipo)) {
            throw new OperacionNoPermitidaException(
                    "Ya registraste tu " + tipo + " de hoy. Máximo una ENTRADA y una SALIDA por día.");
        }
        Asistencia asistencia = new Asistencia(usuario, tipo, hoy, LocalTime.now(zona));
        return AsistenciaResponse.desde(asistenciaRepository.save(asistencia));
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listar() {
        return asistenciaRepository.findAllByOrderByFechaDescHoraDesc().stream()
                .map(AsistenciaResponse::desde)
                .toList();
    }
}
