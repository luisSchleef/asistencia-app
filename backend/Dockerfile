# ===== Etapa 1: build (JDK + Maven) =====
# Las capas van ordenadas pom-primero para que la caché de Docker
# conserve la descarga de dependencias mientras el código cambie.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src src
RUN mvn -q package -DskipTests

# ===== Etapa 2: runtime (solo JRE + el jar) =====
# La imagen final NO lleva Maven, fuentes ni caché: ~200MB en vez de ~700MB.
FROM eclipse-temurin:21-jre-alpine
# tzdata + TZ: sin esto la JVM arranca en UTC y las horas quedan desfasadas.
# wget: lo usa el healthcheck de docker-compose (GET a /v3/api-docs).
RUN apk add --no-cache tzdata wget
ENV TZ=America/Santiago
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# El seeder (@Profile("dev")) no corre: producción con datos limpios.
# Flyway migra el esquema automáticamente en cada arranque.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
