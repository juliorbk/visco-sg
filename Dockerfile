# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar pom primero para aprovechar el caché de capas de Maven
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el resto del código y compilar (sin tests, ya los corres aparte)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S visco && adduser -S visco -G visco
USER visco

# Copiar el JAR generado
COPY --from=builder /app/target/*.jar app.jar

# Anular JAVA_TOOL_OPTIONS que Render inyecta (sobreescribe -Xmx)
ENV JAVA_TOOL_OPTIONS=""

# Puerto que expone Spring Boot
EXPOSE 8080

# Arrancar la app
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-Xmx280m", \
  "-Xms128m", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+UseCompressedOops", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp", \
  "-jar", "app.jar"]