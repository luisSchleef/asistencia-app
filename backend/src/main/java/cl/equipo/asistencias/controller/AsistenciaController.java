package cl.equipo.asistencias.controller;

import cl.equipo.asistencias.dto.AsistenciaResponse;
import cl.equipo.asistencias.model.TipoAsistencia;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.service.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Asistencias")
@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @Operation(summary = "Marcar ENTRADA con fecha/hora actual")
    @PostMapping("/entrada")
    public ResponseEntity<AsistenciaResponse> marcarEntrada(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.marcar(usuario, TipoAsistencia.ENTRADA));
    }

    @Operation(summary = "Marcar SALIDA con fecha/hora actual")
    @PostMapping("/salida")
    public ResponseEntity<AsistenciaResponse> marcarSalida(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.marcar(usuario, TipoAsistencia.SALIDA));
    }

    @Operation(summary = "Historial completo de asistencias")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AsistenciaResponse> listar() {
        return asistenciaService.listar();
    }
}
