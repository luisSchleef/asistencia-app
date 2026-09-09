package cl.equipo.asistencias;

import cl.equipo.asistencias.model.Asistencia;
import cl.equipo.asistencias.model.TipoAsistencia;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.AsistenciaRepository;
import cl.equipo.asistencias.repository.UsuarioRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReporteIntegrationTest {

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    TestRestTemplate rest;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    AsistenciaRepository asistenciaRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    String adminToken;
    String empleadoToken;

    private HttpEntity<Object> entidad(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<List> lista(String url, String token) {
        return rest.exchange(url, HttpMethod.GET, entidad(token, null), List.class);
    }

    @Test
    @Order(1)
    void prepararDatosConHorasConocidas() {
        adminToken = login("admin@empresa.cl", "admin123");
        assertThat(adminToken).isNotBlank();

        usuarioRepository.save(new Usuario("Pepe Empleado", "pepe@empresa.cl",
                passwordEncoder.encode("clave123"), cl.equipo.asistencias.model.Rol.EMPLEADO));
        empleadoToken = login("pepe@empresa.cl", "clave123");
        Usuario pepe = usuarioRepository.findByCorreo("pepe@empresa.cl").orElseThrow();

        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        LocalDate antesDeAyer = hoy.minusDays(2);

        ayer = saltarFinde(ayer);
        antesDeAyer = saltarFinde(antesDeAyer);

        asistenciaRepository.save(new Asistencia(pepe, TipoAsistencia.ENTRADA, ayer, LocalTime.of(9, 45)));
        asistenciaRepository.save(new Asistencia(pepe, TipoAsistencia.ENTRADA, antesDeAyer, LocalTime.of(8, 0)));

        asistenciaRepository.save(new Asistencia(pepe, TipoAsistencia.SALIDA, ayer, LocalTime.of(17, 0)));
        asistenciaRepository.save(new Asistencia(pepe, TipoAsistencia.SALIDA, antesDeAyer, LocalTime.of(18, 0)));
    }

    private static LocalDate saltarFinde(LocalDate dia) {
        return switch (dia.getDayOfWeek()) {
            case SATURDAY -> dia.minusDays(1);
            case SUNDAY -> dia.minusDays(2);
            default -> dia;
        };
    }

    private String login(String correo, String contrasena) {
        ResponseEntity<Map> r = rest.exchange("/api/auth/login", HttpMethod.POST,
                entidad(null, Map.of("correo", correo, "contrasena", contrasena)), Map.class);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        return (String) r.getBody().get("accessToken");
    }

    @Test
    @Order(2)
    void atrasosIncluyeSoloDespuesDelLimite() {
        List<Map> atrasos = lista("/api/reportes/atrasos", adminToken).getBody();
        assertThat(atrasos).isNotNull();
        List<Map> dePepe = atrasos.stream()
                .filter(a -> "Pepe Empleado".equals(a.get("usuarioNombre")))
                .toList();
        assertThat(dePepe).hasSize(1);
        assertThat(dePepe.get(0).get("hora").toString()).startsWith("09:45");
    }

    @Test
    @Order(3)
    void salidasAnticipadasIncluyeSoloAntesDelLimite() {
        List<Map> salidas = lista("/api/reportes/salidas-anticipadas", adminToken).getBody();
        List<Map> dePepe = salidas.stream()
                .filter(a -> "Pepe Empleado".equals(a.get("usuarioNombre")))
                .toList();
        assertThat(dePepe).hasSize(1);
        assertThat(dePepe.get(0).get("hora").toString()).startsWith("17:00");
    }

    @Test
    @Order(4)
    void inasistenciasNoIncluyeFinesDeSemana() {
        List<Map> inasistencias = lista("/api/reportes/inasistencias", adminToken).getBody();
        assertThat(inasistencias).isNotNull();
        for (Map fila : inasistencias) {
            LocalDate fecha = LocalDate.parse(fila.get("fecha").toString());
            assertThat(fecha.getDayOfWeek()).isNotIn(List.of(
                    java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY));
        }
    }

    @Test
    @Order(5)
    void inasistenciasReportaDiaHabilSinMarcacion() {
        List<Map> inasistencias = lista("/api/reportes/inasistencias", adminToken).getBody();

        List<Map> deAdmin = inasistencias.stream()
                .filter(i -> "Admin".equals(i.get("usuarioNombre")))
                .toList();
        assertThat(deAdmin).isNotEmpty();
    }

    @Test
    @Order(6)
    void empleadoNoPuedeVerReportes403() {
        for (String url : List.of("/api/reportes/atrasos", "/api/reportes/salidas-anticipadas",
                "/api/reportes/inasistencias")) {
            ResponseEntity<Map> r = rest.exchange(url, HttpMethod.GET,
                    entidad(empleadoToken, null), Map.class);
            assertThat(r.getStatusCode().value()).as(url).isEqualTo(403);
        }
    }

    @Test
    @Order(7)
    void sinTokenRechaza401() {
        ResponseEntity<Map> r = rest.exchange("/api/reportes/atrasos", HttpMethod.GET,
                entidad(null, null), Map.class);
        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }
}
