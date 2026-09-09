package cl.equipo.asistencias.config;

import cl.equipo.asistencias.model.Asistencia;
import cl.equipo.asistencias.model.Rol;
import cl.equipo.asistencias.model.TipoAsistencia;
import cl.equipo.asistencias.model.Usuario;
import cl.equipo.asistencias.repository.AsistenciaRepository;
import cl.equipo.asistencias.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
@Profile("dev")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    record Semilla(String nombre, String correo, Rol rol) {}

    @Bean
    CommandLineRunner seed(UsuarioRepository usuarioRepository,
                           AsistenciaRepository asistenciaRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuarioRepository.count() > 1) {
                log.info("DataSeeder: la BD ya tiene datos, no se vuelve a sembrar");
                return;
            }

            SecureRandom azar = new SecureRandom();
            List<Semilla> semillas = List.of(
                    new Semilla("Admin", "admin@empresa.cl", Rol.ADMIN),
                    new Semilla("Luis Torres", "luis@empresa.cl", Rol.ADMIN),
                    new Semilla("Javi Ríos", "javi@empresa.cl", Rol.EMPLEADO),
                    new Semilla("Ana Soto", "ana@empresa.cl", Rol.EMPLEADO),
                    new Semilla("Pedro Vega", "pedro@empresa.cl", Rol.EMPLEADO),
                    new Semilla("Camila Ruiz", "camila@empresa.cl", Rol.EMPLEADO));

            LocalDate hoy = LocalDate.now();
            for (Semilla s : semillas) {
                boolean existe = usuarioRepository.existsByCorreo(s.correo());
                Usuario usuario = existe
                        ? usuarioRepository.findByCorreo(s.correo()).orElseThrow()
                        : usuarioRepository.save(new Usuario(
                                s.nombre(), s.correo(),
                                passwordEncoder.encode("clave123"), s.rol()));

                if (existe) continue;

                for (int i = 1; i <= 15; i++) {
                    LocalDate dia = hoy.minusDays(i);
                    if (dia.getDayOfWeek() == DayOfWeek.SATURDAY
                            || dia.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                    if (azar.nextInt(10) < 2) continue;

                    int horaEntrada = 8 + azar.nextInt(2);
                    asistenciaRepository.save(new Asistencia(usuario, TipoAsistencia.ENTRADA,
                            dia, LocalTime.of(horaEntrada, azar.nextInt(60))));
                    if (azar.nextInt(10) < 8) {

                        int offsetSalida = azar.nextInt(120);
                        asistenciaRepository.save(new Asistencia(usuario, TipoAsistencia.SALIDA,
                                dia, LocalTime.of(16, 30).plusMinutes(offsetSalida)));
                    }
                }
            }
            log.info("DataSeeder: datos de prueba listos (usuarios + asistencias de 15 días)");
        };
    }
}
