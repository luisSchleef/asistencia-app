# Auditoría estática — Asistencias App

**Alcance:** backend Spring Boot, frontend React, migraciones y Docker/nginx.  
**Método:** revisión estática del código y configuración; no se modificaron archivos de la aplicación ni se ejecutó el stack, Testcontainers o un escáner SCA.

## Resumen ejecutivo

La arquitectura base es sólida: JWT firmado, BCrypt, autorización de roles aplicada en backend, Flyway con `ddl-auto: validate`, consultas que evitan N+1 en rutas relevantes y una base de datos no expuesta al host.

Sin embargo, el proyecto **no está preparado para producción sin cambios**. Los riesgos principales son:

1. Una cuenta `ADMIN` con contraseña pública y permanente se inserta en cada base nueva.
2. Los valores secretos inseguros tienen un *fallback* funcional si falta `.env`.
3. La regla «una entrada/salida por día» no está garantizada por la base de datos y falla ante solicitudes concurrentes.
4. Los JWT se persisten en `localStorage` y nginx no aporta CSP ni cabeceras de defensa.

## Hallazgos priorizados

| Prioridad | Hallazgo | Impacto |
|---|---|---|
| **Crítica** | Admin inicial conocido: `admin@empresa.cl` / `admin123` | Toma completa de control en instalaciones nuevas si no se cambia inmediatamente. |
| **Alta** | Secretos y contraseña de BD por defecto funcionales | JWT falsificables y acceso a BD si se despliega sin configuración correcta. |
| **Alta** | Sin restricción única para asistencias | Duplicados bajo concurrencia; se rompe la integridad del registro horario. |
| **Alta** | JWT en `localStorage` + falta de CSP/cabeceras | Un XSS futuro permitiría robar sesiones activas. |
| **Media** | Login sin rate limiting ni bloqueo progresivo | Fuerza bruta de credenciales, especialmente sobre la cuenta admin conocida. |
| **Media** | No se revocan sesiones al cambiar contraseña | Un token previamente robado sigue operativo hasta ocho horas. |
| **Media** | HTTP sin TLS y backend publicado directamente | Credenciales y tokens pueden interceptarse si se expone fuera de localhost. |
| **Media** | Borrado de usuarios elimina su historial por `CASCADE` | Pérdida irreversible de trazabilidad/auditoría. |
| **Media** | Reporte de inasistencias es semánticamente impreciso y no escala | No detecta ausencias previas a la primera marca; complejidad creciente con el historial. |
| **Baja** | Falta de paginación/filtros en listados y reportes | Riesgo de latencia/memoria cuando aumenten los registros. |
| **Baja** | Sin tests frontend y tests backend dependientes del orden | Menor protección contra regresiones. |
| **Baja** | Accesibilidad y robustez de frontend mejorables | Modal sin foco atrapado, pestañas no semánticas y HTTP sin timeout. |

## Hallazgos detallados

### 1. Crítica — Credenciales administrativas públicas en una migración de producción

**Evidencia**

- `backend/src/main/resources/db/migration/V2__datos_semilla.sql:1-6` inserta siempre:
  - correo: `admin@empresa.cl`
  - contraseña: `admin123`
  - rol: `ADMIN`
- El README publica las mismas credenciales en `README.md:79-84`.

**Impacto**

Flyway aplica esta migración en toda base de datos nueva, incluidos despliegues sin perfil `dev`. Un atacante que alcance el login conoce una cuenta administradora válida.

El texto «cambiar al primer uso» no es un control: no hay obligación técnica de cambio, expiración ni proceso de bootstrap seguro.

**Recomendación**

- Eliminar el usuario admin de la migración de producción.
- Implementar un proceso de bootstrap controlado:
  - variables obligatorias como `INITIAL_ADMIN_EMAIL` y `INITIAL_ADMIN_PASSWORD`, o
  - creación mediante CLI/operación administrativa segura.
- Marcar la cuenta como «debe cambiar contraseña» sólo si existe un flujo que realmente lo fuerce.
- Si ya se desplegó: **cambiar esa contraseña inmediatamente** o eliminar/reemplazar el usuario, según corresponda.

### 2. Alta — Secretos y credenciales inseguros por defecto

**Evidencia**

- `backend/src/main/resources/application.yml:4-7` usa `asistencias` como usuario y contraseña de respaldo de PostgreSQL.
- `backend/src/main/resources/application.yml:20-24` define un JWT secreto conocido como fallback.
- `docker-compose.yml:13-15` y `docker-compose.yml:33-36` repiten dichos valores.
- El README afirma que el stack arranca sin `.env` en `README.md:52`.

**Impacto**

Un despliegue accidental sin `.env` válido produce tokens JWT firmables por cualquiera que conozca el repositorio. Aunque `setup.sh` intenta generar un secreto, el uso manual de Docker Compose sigue permitiendo los fallbacks.

**Recomendación**

- Separar perfiles/configuraciones explícitas: `dev` puede tener defaults, `prod` debe requerir variables.
- En producción, fallar durante el arranque si `JWT_SECRET`, `DB_PASSWORD` u otros secretos no están definidos o no cumplen longitud/entropía mínima.
- No usar secretos conocidos en `docker-compose.yml` de producción.
- Usar un gestor de secretos del entorno de despliegue cuando esté disponible.

### 3. Alta — La regla de marcación no es atómica y carece de restricción en BD

**Evidencia**

- `backend/src/main/java/cl/equipo/asistencias/service/AsistenciaService.java:31-36` verifica primero `existsByUsuarioAndFechaAndTipo(...)` y después inserta.
- `backend/src/main/resources/db/migration/V1__esquema.sql:13-22` no define `UNIQUE (usuario_id, fecha, tipo)`.

**Impacto**

Dos solicitudes concurrentes pueden observar que aún no existe una marca y ambas insertar una `ENTRADA` o `SALIDA`. La regla se cumple en la aplicación sólo bajo baja concurrencia, no como garantía de datos.

**Recomendación**

Crear una nueva migración Flyway:

```sql
ALTER TABLE asistencias
  ADD CONSTRAINT uq_asistencias_usuario_fecha_tipo
  UNIQUE (usuario_id, fecha, tipo);
```

Además:

- Mantener la comprobación de negocio para entregar un mensaje claro.
- Capturar `DataIntegrityViolationException` y traducirla a `409 Conflict`.
- Añadir un test de concurrencia que confirme que sólo se persiste una marca.

### 4. Alta — Token en `localStorage` y ausencia de cabeceras HTTP defensivas

**Evidencia**

- El estado de sesión se persiste en `localStorage` en `frontend/src/store/auth.ts`.
- Axios toma ese token y lo envía como `Bearer` en `frontend/src/api/client.ts`.
- `frontend/nginx.conf:3-20` no configura CSP, `X-Content-Type-Options`, política de framing, referrer o permissions.
- `frontend/index.html:9-19` contiene un script inline, que debe migrarse o hashearse antes de imponer una CSP estricta.

**Impacto**

No se detectaron sinks XSS directos como `dangerouslySetInnerHTML` o `eval`, lo cual es positivo. Pero una futura inyección XSS o dependencia comprometida podría leer y exfiltrar el bearer token almacenado en JavaScript.

**Recomendación**

1. Preferir sesiones mediante cookie `HttpOnly; Secure; SameSite=Lax/Strict`.
2. Si se usan cookies en métodos mutables, implementar protección CSRF.
3. Como alternativa transitoria, mantener el access token sólo en memoria y usar refresh token rotativo en cookie `HttpOnly`.
4. Añadir en nginx una CSP estricta y cabeceras como:
   - `Content-Security-Policy`
   - `X-Content-Type-Options: nosniff`
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - `Permissions-Policy`
   - `X-Frame-Options: DENY` o `frame-ancestors 'none'` dentro de CSP.

### 5. Media — Login sin protección contra fuerza bruta

**Evidencia**

- El login público se habilita en `backend/src/main/java/cl/equipo/asistencias/security/SecurityConfig.java:49-52`.
- `backend/src/main/java/cl/equipo/asistencias/service/AuthService.java:33-40` valida correctamente credenciales de forma genérica, pero no limita intentos.

**Impacto**

Las contraseñas pueden probarse repetidamente. El riesgo es más elevado mientras exista la cuenta admin por defecto.

**Recomendación**

- Rate limiting por IP y, de forma prudente, por identificador de cuenta.
- Backoff progresivo o bloqueo temporal tras varios fallos.
- Registro de intentos fallidos y alertas operativas.
- Evitar respuestas que revelen si la cuenta existe; esto ya está bien aplicado con `CredencialesInvalidasException`.

### 6. Media — Cambio de contraseña no invalida JWT existentes

**Evidencia**

- Los tokens tienen ocho horas de vigencia por defecto: `backend/src/main/resources/application.yml:22-24`.
- El token sólo contiene sujeto, nombre y rol: `backend/src/main/java/cl/equipo/asistencias/security/JwtService.java:28-37`.
- Actualizar contraseña sólo cambia el hash: `backend/src/main/java/cl/equipo/asistencias/service/UsuarioService.java:57-59`.
- El filtro vuelve a cargar el usuario y el rol desde BD: `backend/src/main/java/cl/equipo/asistencias/security/JwtAuthFilter.java:44-53`.

**Impacto**

Un JWT emitido antes de cambiar la contraseña continúa siendo válido hasta expirar, salvo que se elimine el usuario. La recarga de rol sí evita que un token antiguo conserve privilegios `ADMIN` tras un descenso de rol.

**Recomendación**

- Implementar `tokenVersion`, `passwordChangedAt` o una lista de revocación por `jti`.
- Incluir el dato relevante en el JWT y compararlo con el estado actual de la cuenta.
- Reducir vida del access token e incorporar refresh tokens rotativos si se necesita una buena experiencia de sesión.

### 7. Media — Despliegue sin TLS y API publicada directamente

**Evidencia**

- nginx escucha HTTP: `frontend/nginx.conf:4`.
- Se publica `5173:80`: `docker-compose.yml:53-57`.
- El backend se publica en `8080`: `docker-compose.yml:39-40`.

**Impacto**

Si los puertos se exponen más allá de localhost sin un reverse proxy TLS externo, usuario, contraseña y JWT viajan sin cifrado. La API también puede consumirse directamente sin atravesar nginx.

**Recomendación**

- Para producción, terminar TLS en un proxy dedicado o nginx.
- Añadir HSTS sólo una vez HTTPS esté correctamente activo.
- No publicar `backend:8080` salvo que haya una necesidad concreta; conservarlo dentro de la red Docker.
- Mantener una configuración claramente distinta entre demo/desarrollo y producción.

### 8. Media — Borrado destructivo de usuario elimina el historial de asistencia

**Evidencia**

- La FK usa `ON DELETE CASCADE`: `backend/src/main/resources/db/migration/V1__esquema.sql:13-19`.
- El servicio confirma este comportamiento: `backend/src/main/java/cl/equipo/asistencias/service/UsuarioService.java:63-71`.

**Impacto**

Eliminar una persona borra todas sus marcas. Para un sistema de asistencia, esto reduce trazabilidad, puede invalidar reportes históricos y puede ser incompatible con políticas de retención aplicables.

**Recomendación**

- Preferir baja lógica (`activo = false`, `fechaBaja`) y excluir usuarios inactivos de operaciones futuras.
- Preservar registros históricos y auditar cambios administrativos.
- Si el borrado físico debe existir, restringirlo a cuentas sin registros o a una operación excepcional y trazada.

### 9. Media — Cálculo de inasistencias no representa correctamente el periodo laboral

**Evidencia**

- El cálculo comienza en la primera asistencia de cada usuario: `backend/src/main/java/cl/equipo/asistencias/service/ReporteService.java:64-105`.
- Para quien jamás marcó, usa como inicio la primera marca global: `backend/src/main/java/cl/equipo/asistencias/service/ReporteService.java:86-96`.
- La entidad `Usuario` no registra fecha de contratación/alta: `backend/src/main/java/cl/equipo/asistencias/model/Usuario.java:24-53`.

**Impacto**

- Las ausencias entre la fecha real de contratación y la primera marca no aparecen.
- Un usuario nuevo sin marcaciones se evalúa desde una fecha global que puede no corresponder a su incorporación.
- El algoritmo recorre cada día por usuario en memoria; el coste crecerá con años de historial y número de empleados.

**Recomendación**

- Añadir `fechaIngreso` y, si aplica, `fechaBaja` al dominio.
- Definir reportes por rango de fechas solicitado.
- Excluir feriados y considerar calendarios/turnos reales si el negocio lo requiere.
- Evaluar una consulta SQL con generación de series de fechas (`generate_series`) o un reporte paginado por rango.

## Calidad, pruebas y operación

### Aspectos positivos

- Contraseñas se almacenan con BCrypt: `backend/src/main/java/cl/equipo/asistencias/security/SecurityConfig.java:38-41`.
- JWT se verifica criptográficamente y expira: `backend/src/main/java/cl/equipo/asistencias/security/JwtService.java:40-46`.
- Los roles se vuelven a cargar desde BD por petición, evitando privilegios obsoletos.
- El backend aplica autorización real con `@PreAuthorize`, no depende de la UI.
- El modelo Flyway + `ddl-auto: validate` es una buena garantía de consistencia del esquema.
- Se usan `EntityGraph` y `JOIN FETCH` para evitar N+1 en rutas principales.
- El frontend emplea `pnpm install --frozen-lockfile`, y el backend usa pruebas de integración con PostgreSQL real mediante Testcontainers.

### Mejoras recomendadas

- Añadir paginación, filtros por fecha/usuario y límites máximos a:
  - `backend/src/main/java/cl/equipo/asistencias/service/AsistenciaService.java:40-45`
  - reportes administrativos.
- Evitar que las pruebas dependan de orden y estado compartido:
  - `backend/src/test/java/cl/equipo/asistencias/ApiIntegrationTest.java:31-48`
  - `backend/src/test/java/cl/equipo/asistencias/ReporteIntegrationTest.java:39-60`
- Añadir pruebas frontend: login, expiración/401, guards por rol, marcaciones y componentes accesibles.
- Configurar timeout global para Axios; actualmente el cliente puede quedar esperando indefinidamente si la API no responde.
- Mejorar accesibilidad:
  - El modal necesita gestión/restauración de foco y *focus trap*.
  - Las pestañas de reportes deberían usar el patrón WAI-ARIA Tabs o botones convencionales.
  - Los errores de formularios deben asociarse con `aria-invalid` y `aria-describedby`.
- Fijar versiones/digests de imágenes Docker en vez de etiquetas móviles como `nginx:alpine` y `node:22-alpine`.
- Añadir CI para `./mvnw test`, `pnpm run build`, `pnpm run lint`, escaneo de dependencias e imágenes.

## Diagnósticos del editor

El diagnóstico global informó **2 errores en `backend`**, pero no incluyó los archivos/líneas. Se verificaron los archivos de seguridad, excepciones y asistencia revisados y no presentan diagnósticos locales. No se ejecutó Maven, por lo que no se puede confirmar su causa desde esta auditoría.

## Plan de remediación sugerido

1. **Antes de cualquier despliegue público:** eliminar el admin fijo, rotar contraseña/secretos y exigir configuración productiva.
2. **Integridad:** agregar la restricción única de asistencias y manejar el conflicto de concurrencia.
3. **Perímetro:** TLS, retirar exposición directa del backend, CSP y cabeceras de seguridad.
4. **Sesiones:** migrar fuera de `localStorage`, o aplicar una estrategia transitoria de tokens cortos/revocables.
5. **Dominio y escalabilidad:** baja lógica, fechas de vigencia laboral y reportes filtrados/paginados.
6. **Automatización:** pruebas frontend, CI y escaneo SCA/container.
