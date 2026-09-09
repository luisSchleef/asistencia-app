package cl.equipo.asistencias;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection

    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    TestRestTemplate rest;

    static String adminToken;
    static String empleadoToken;
    static Long empleadoId;

    private ResponseEntity<Map> post(String url, String token, Object body) {
        return rest.exchange(url, HttpMethod.POST, entidad(token, body), Map.class);
    }

    private ResponseEntity<Map> get(String url, String token) {
        return rest.exchange(url, HttpMethod.GET, entidad(token, null), Map.class);
    }

    private HttpEntity<Object> entidad(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private String login(String correo, String contrasena) {
        ResponseEntity<Map> r = post("/api/auth/login", null,
                Map.of("correo", correo, "contrasena", contrasena));
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        return (String) r.getBody().get("accessToken");
    }

    @Test
    @Order(1)
    void loginAdminSemillaRetornaToken() {
        adminToken = login("admin@empresa.cl", "admin123");
        assertThat(adminToken).isNotBlank().contains(".");
    }

    @Test
    @Order(2)
    void loginContrasenaIncorrectaRechaza401() {
        ResponseEntity<Map> r = post("/api/auth/login", null,
                Map.of("correo", "admin@empresa.cl", "contrasena", "otra"));
        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void endpointProtegidoSinTokenRechaza401() {
        ResponseEntity<Map> r = get("/api/asistencias", null);
        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(4)
    void meRetornaDatosDelToken() {
        ResponseEntity<Map> r = get("/api/auth/me", adminToken);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody().get("rol")).isEqualTo("ADMIN");
    }

    @Test
    @Order(5)
    void adminCreaEmpleado() {
        ResponseEntity<Map> r = post("/api/usuarios", adminToken, Map.of(
                "nombre", "Javi", "correo", "javi@empresa.cl",
                "contrasena", "clave123", "rol", "EMPLEADO"));
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        empleadoId = ((Number) r.getBody().get("id")).longValue();
        empleadoToken = login("javi@empresa.cl", "clave123");
    }

    @Test
    @Order(6)
    void correoDuplicadoRechaza409() {
        ResponseEntity<Map> r = post("/api/usuarios", adminToken, Map.of(
                "nombre", "Otro", "correo", "javi@empresa.cl",
                "contrasena", "clave123", "rol", "EMPLEADO"));
        assertThat(r.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @Order(7)
    void datosInvalidosRechaza400ConDetalle() {
        ResponseEntity<Map> r = post("/api/usuarios", adminToken, Map.of(
                "nombre", "", "correo", "no-es-correo",
                "contrasena", "123", "rol", "EMPLEADO"));
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsKey("campos");
    }

    @Test
    @Order(8)
    void empleadoMarcaEntradaYAdminVeElHistorial() {
        ResponseEntity<Map> marcada = post("/api/asistencias/entrada", empleadoToken, null);
        assertThat(marcada.getStatusCode().value()).isEqualTo(201);
        assertThat(marcada.getBody().get("tipo")).isEqualTo("ENTRADA");

        ResponseEntity<Map> repetida = post("/api/asistencias/entrada", empleadoToken, null);
        assertThat(repetida.getStatusCode().value()).isEqualTo(409);

        ResponseEntity<Map> salida = post("/api/asistencias/salida", empleadoToken, null);
        assertThat(salida.getStatusCode().value()).isEqualTo(201);
        assertThat(post("/api/asistencias/salida", empleadoToken, null).getStatusCode().value()).isEqualTo(409);

        ResponseEntity<List> historial = rest.exchange("/api/asistencias", HttpMethod.GET,
                entidad(adminToken, null), List.class);
        assertThat(historial.getStatusCode().value()).isEqualTo(200);
        assertThat(historial.getBody()).isNotEmpty();
        assertThat(((Map) historial.getBody().get(0)).get("usuarioNombre")).isEqualTo("Javi");
    }

    @Test
    @Order(9)
    void empleadoNoPuedeListarUsuarios403() {
        ResponseEntity<Map> r = get("/api/usuarios", empleadoToken);
        assertThat(r.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @Order(10)
    void adminNoPuedeEliminarseASiMismo409() {

        ResponseEntity<Map> r = rest.exchange("/api/usuarios/1", HttpMethod.DELETE,
                entidad(adminToken, null), Map.class);
        assertThat(r.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @Order(11)
    void eliminarEmpleadoConCascade() {
        ResponseEntity<Map> r = rest.exchange("/api/usuarios/" + empleadoId, HttpMethod.DELETE,
                entidad(adminToken, null), Map.class);
        assertThat(r.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<List> historial = rest.exchange("/api/asistencias", HttpMethod.GET,
                entidad(adminToken, null), List.class);

        assertThat(historial.getBody()).isEmpty();
    }
}
