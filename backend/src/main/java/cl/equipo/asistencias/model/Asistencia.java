package cl.equipo.asistencias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAsistencia tipo;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(nullable = false)
    private LocalTime hora;

    public Asistencia(Usuario usuario, TipoAsistencia tipo, LocalDate fecha, LocalTime hora) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.fecha = fecha;
        this.hora = hora;
    }
}
