CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    rol VARCHAR(255) NOT NULL CHECK (rol IN ('ADMIN', 'EMPLEADO'))
);

CREATE TABLE asistencias (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo VARCHAR(255) NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    fecha DATE NOT NULL,
    hora TIME NOT NULL
);

CREATE INDEX idx_asistencias_usuario ON asistencias(usuario_id);
CREATE INDEX idx_asistencias_fecha ON asistencias(fecha);
