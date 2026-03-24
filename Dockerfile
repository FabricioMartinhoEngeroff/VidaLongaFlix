# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package && \
    JAR_PATH="$(ls -1 target/*.jar | grep -v '\\.original$' | head -n 1)" && \
    test -n "$JAR_PATH" && \
    cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

# Rule 2: create a non-root user — limits blast radius if container is compromised
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

EXPOSE 8090

COPY --from=build /workspace/app.jar ./app.jar

# exec replaces the shell with java as PID 1, so SIGTERM reaches the JVM for graceful shutdown
ENV JAVA_OPTS=""
USER appuser
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
