package cl.equipo.asistencias.service;

import cl.equipo.asistencias.dto.AsistenciaResponse;
import cl.equipo.asistencias.dto.InasistenciaResponse;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.AsistenciaRepository;
import cl.equipo.asistencias.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@Service
public class ReporteService {

    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LocalTime limiteEntrada;
    private final LocalTime limiteSalida;

    public ReporteService(AsistenciaRepository asistenciaRepository,
                          UsuarioRepository usuarioRepository,

                          @Value("${app.horario.entrada}") String limiteEntrada,
                          @Value("${app.horario.salida}") String limiteSalida) {
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.limiteEntrada = LocalTime.parse(limiteEntrada, DateTimeFormatter.ofPattern("HH:mm"));
        this.limiteSalida = LocalTime.parse(limiteSalida, DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> atrasos() {
        return asistenciaRepository.atrasos(limiteEntrada).stream()
                .map(AsistenciaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> salidasAnticipadas() {
        return asistenciaRepository.salidasAnticipadas(limiteSalida).stream()
                .map(AsistenciaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InasistenciaResponse> inasistencias() {
        Map<Long, Set<LocalDate>> diasPorUsuario = new LinkedHashMap<>();
        Map<Long, LocalDate> primerDiaPorUsuario = new LinkedHashMap<>();

        for (Object[] fila : asistenciaRepository.fechasConMarcacion()) {
            Long usuarioId = (Long) fila[0];
            LocalDate fecha = (LocalDate) fila[1];
            diasPorUsuario.computeIfAbsent(usuarioId, k -> new HashSet<>()).add(fecha);
            primerDiaPorUsuario.merge(usuarioId, fecha,
                    (actual, nueva) -> nueva.isBefore(actual) ? nueva : actual);
        }

        LocalDate ayer = LocalDate.now(ZoneId.of("America/Santiago")).minusDays(1);

        LocalDate inicioGlobal = primerDiaPorUsuario.values().stream()
                .min(LocalDate::compareTo)
                .orElse(ayer);

        List<InasistenciaResponse> resultado = new ArrayList<>();
        for (Usuario usuario : usuarioRepository.findAll()) {
            Set<LocalDate> marcados = diasPorUsuario.getOrDefault(usuario.getId(), Set.of());
            LocalDate desde = primerDiaPorUsuario.getOrDefault(usuario.getId(), inicioGlobal);
            for (LocalDate dia = desde; !dia.isAfter(ayer); dia = dia.plusDays(1)) {
                DayOfWeek dw = dia.getDayOfWeek();
                if (dw == DayOfWeek.SATURDAY || dw == DayOfWeek.SUNDAY) continue;
                if (!marcados.contains(dia)) {
                    resultado.add(new InasistenciaResponse(usuario.getId(), usuario.getNombre(), dia));
                }
            }
        }
        return resultado;
    }
}
