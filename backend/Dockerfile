# ============================================================================
# DriveAI Backend — Build multi-etapa
#   Etapa 1: compila el fat-jar con Maven + Temurin 17
#   Etapa 2: imagen runtime minima con JRE 17
# ============================================================================

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Descarga dependencias en capa separada para optimizar caché.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# wget se usa en el healthcheck de docker-compose.
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/target/driveai-backend.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]