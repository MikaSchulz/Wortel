####################
# Stage 1: build
####################
FROM gradle:9.3.0-jdk25 AS build
WORKDIR /src
# Cache dependencies first
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle gradle
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true
# Copy sources
COPY src src
RUN gradle --no-daemon clean bootJar

####################
# Stage 2: runtime
####################
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
