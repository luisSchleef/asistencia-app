package cl.equipo.asistencias.controller;

import cl.equipo.asistencias.dto.AsistenciaResponse;
import cl.equipo.asistencias.dto.InasistenciaResponse;
import cl.equipo.asistencias.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reportes")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Entradas después del horario límite (default 09:30)")
    @GetMapping("/atrasos")
    public List<AsistenciaResponse> atrasos() {
        return reporteService.atrasos();
    }

    @Operation(summary = "Salidas antes del horario límite (default 17:30)")
    @GetMapping("/salidas-anticipadas")
    public List<AsistenciaResponse> salidasAnticipadas() {
        return reporteService.salidasAnticipadas();
    }

    @Operation(summary = "Días hábiles (lun-vie) sin ninguna marcación")
    @GetMapping("/inasistencias")
    public List<InasistenciaResponse> inasistencias() {
        return reporteService.inasistencias();
    }
}
