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

EXPOSE 8090

COPY --from=build /workspace/app.jar ./app.jar

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

