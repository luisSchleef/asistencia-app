package cl.equipo.asistencias.repository;

import cl.equipo.asistencias.model.Asistencia;
import cl.equipo.asistencias.model.TipoAsistencia;
import cl.equipo.asistencias.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByUsuarioOrderByFechaDescHoraDesc(Usuario usuario);

    List<Asistencia> findByTipoOrderByFechaDescHoraDesc(TipoAsistencia tipo);

    List<Asistencia> findByFechaBetween(LocalDate desde, LocalDate hasta);

    boolean existsByUsuarioAndFechaAndTipo(Usuario usuario, LocalDate fecha, TipoAsistencia tipo);

    @EntityGraph(attributePaths = "usuario")
    List<Asistencia> findAllByOrderByFechaDescHoraDesc();

    @Query("""
            SELECT a FROM Asistencia a JOIN FETCH a.usuario
            WHERE a.tipo = cl.equipo.asistencias.model.TipoAsistencia.ENTRADA
              AND a.hora > :limite
            ORDER BY a.fecha DESC, a.hora DESC
            """)
    List<Asistencia> atrasos(@Param("limite") LocalTime limite);

    @Query("""
            SELECT a FROM Asistencia a JOIN FETCH a.usuario
            WHERE a.tipo = cl.equipo.asistencias.model.TipoAsistencia.SALIDA
              AND a.hora < :limite
            ORDER BY a.fecha DESC, a.hora DESC
            """)
    List<Asistencia> salidasAnticipadas(@Param("limite") LocalTime limite);

    @Query("SELECT a.usuario.id, a.fecha FROM Asistencia a GROUP BY a.usuario.id, a.fecha")
    List<Object[]> fechasConMarcacion();
}
