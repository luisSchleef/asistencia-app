# Asistencias App

Sistema de control de asistencia para una pequeña empresa: API REST con **Spring Boot 4** (Java 21) + **PostgreSQL**, frontend SPA con **React 19 + TypeScript**, todo dockerizado con **Docker Compose**.

Proyecto evolucionado desde un MVP de escritorio (Java Swing + SQLite) hacia una arquitectura web de tres capas.

- **Web**: https://frontend-flame-six-2nkut5o8mg.vercel.app/login

## Arquitectura

```text
┌──────────────┐     http://localhost:5173      ┌─────────────┐
│   Navegador  │ ─────────────────────────────► │ nginx       │
│  (React SPA) │         HTML + estáticos       │ (frontend)  │
└──────────────┘                                └──────┬──────┘
                                                       │ proxy /api → backend:8080
                                                       │ (mismo origen → sin CORS)
                                                ┌──────▼──────┐
                                                │  Spring     │
                                                │  Boot (API) │
                                                └──────┬──────┘
                                                       │ JDBC + Hibernate
                                                ┌──────▼──────┐
                                                │ PostgreSQL  │
                                                │  (volumen)  │
                                                └─────────────┘
```

| Pieza | Tecnología | Detalle |
|---|---|---|
| Frontend | React 19, TypeScript, Vite, React Router 7, Zustand, Axios (pnpm) | SPA con rutas protegidas por rol; interceptor JWT |
| Backend | Spring Boot 4, Java 21, Spring Security, JPA/Hibernate, Flyway, springdoc | API REST con JWT (HMAC-SHA512), roles ADMIN/EMPLEADO con `@PreAuthorize` |
| Base de datos | PostgreSQL 16 | Migraciones versionadas (Flyway), esquema validado por Hibernate |
| Infraestructura | Docker Compose, nginx, Testcontainers (tests) | Multi-stage builds; proxy nginx elimina CORS |

## Cómo correr

### Con Docker (todo el stack)

Requiere Docker + Compose v2 y puertos libres `5173` y `8080`.

```bash
./setup.sh
# Crea .env si falta, genera JWT_SECRET y ejecuta docker compose up -d --build
```

> Sin `.env` el stack igual arranca con valores por defecto (solo demo local).

- **App web**: http://localhost:5173
- **API/Swagger**: http://localhost:8080/swagger-ui/index.html
- Parar: `docker compose down` (los datos persisten en el volumen `pgdata`)
- Borrar datos: `docker compose down -v` (cuidado: elimina la BD)

## Deploy demo en la nube

La demo desplegada separa frontend y backend en dos plataformas:

```text
┌──────────────┐  HTTPS (CORS)   ┌────────────────────────────┐  JDBC   ┌──────────────────────┐
│ Vercel       │ ──────────────► │ Railway                    │ ──────► │ Railway PostgreSQL   │
│ (React SPA)  │                 │ Spring Boot (Dockerfile)   │         │ (Flyway al arrancar) │
└──────────────┘                 └────────────────────────────┘         └──────────────────────┘
```

| Pieza | Plataforma | Configuración |
|---|---|---|
| Frontend (SPA) | Vercel |
| Backend (API) | Railway |
| Base de datos | Railway PostgreSQL |

## Usuarios iniciales

| Correo | Contraseña | Rol | Origen |
|---|---|---|---|
| `admin@empresa.cl` | `admin123` | ADMIN | migración V2 (cambiar al primer uso) |

## API (resumen)

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/login` | público | Devuelve JWT (8h) |
| `GET /api/auth/me` | autenticado | Datos del usuario del token |
| `POST /api/asistencias/entrada` · `/salida` | autenticado | Marca con fecha/hora del servidor |
| `GET /api/asistencias` | ADMIN | Historial completo |
| `GET /api/reportes/atrasos` | ADMIN | Entradas después del límite (default 09:30, configurable) |
| `GET /api/reportes/salidas-anticipadas` | ADMIN | Salidas antes del límite (default 17:30) |
| `GET /api/reportes/inasistencias` | ADMIN | Días hábiles sin marcación |
| CRUD `/api/usuarios` | ADMIN | Crear / listar / actualizar / eliminar (no auto-eliminación) |

Errores JSON consistentes: 400 (validación con detalle por campo), 401, 403, 404, 409 (correo duplicado, auto-eliminación).

## Estructura del monorepo

```text
asistencias-app/
├── backend/
│   ├── src/main/java/cl/equipo/asistencias/
│   │   ├── controller/    # endpoints REST (+ @PreAuthorize por rol)
│   │   ├── service/       # reglas de negocio (+ @Transactional)
│   │   ├── repository/    # Spring Data JPA (derivadas + JPQL)
│   │   ├── model/         # entidades (Usuario, Asistencia) + enums
│   │   ├── security/      # JWT: service, filtro por request, SecurityConfig
│   │   ├── dto/           # records de entrada/salida con Bean Validation
│   │   ├── exception/     # excepciones de dominio + handler global → JSON
│   │   └── config/        # DataSeeder (perfil dev)
│   ├── src/main/resources/db/migration/   # Flyway: V1 esquema, V2 semilla
│   ├── src/test/java/     # ApiIntegrationTest, ReporteIntegrationTest
│   └── Dockerfile         # multi-stage: Maven → JRE
├── frontend/
│   ├── src/
│   │   ├── api/           # cliente Axios (interceptor JWT) + endpoints tipados
│   │   ├── store/         # Zustand: sesión persistida en localStorage
│   │   ├── components/    # guards de ruta (ProtectedRoute, AdminRoute)
│   │   ├── pages/         # Login, Menú, Asistencias, Reportes, Usuarios
│   │   └── types/         # espejo TS de los DTOs del backend
│   ├── nginx.conf         # proxy /api → backend + SPA fallback
│   ├── vercel.json        # SPA fallback para el deploy en Vercel
│   └── Dockerfile         # multi-stage: Node build → nginx
├── docker-compose.yml     # postgres + backend + frontend (red interna, healthcheck)
├── .env.example           # plantilla de secretos (.env está en .gitignore)
├── setup.sh               # bootstrap: crea .env, genera JWT y levanta el stack
└── README.md
```

## Decisiones de diseño destacadas

- **JWT stateless + rol recargado por petición**: el token viaja en cada request, pero el rol se lee de la BD (revocación de permisos instantánea).
- **Autorización en dos capas**: la UI oculta lo admin (guards de ruta) y el backend lo obliga (`@PreAuthorize`).
- **Mismo origen por defecto**: en dev el proxy de Vite reenvía `/api`; en el deploy dockerizado lo hace nginx, sin CORS. Solo el deploy en la nube (SPA en Vercel + API en Railway) necesita CORS, configurado vía `CORS_ORIGINS`.
- **Esquema solo por migraciones**: `ddl-auto: validate` garantiza que el código JPA y el esquema Flyway estén siempre sincronizados.
- **Datos de prueba aislados**: el seeder solo existe bajo el perfil `dev`; producción nunca los recibe.

## Posibles mejoras (roadmap)

- Refresh tokens y revocación de sesión
- TanStack Query para el data-fetching del frontend
- CI (GitHub Actions): build + tests en cada PR
- Exportación de reportes a CSV/PDF
