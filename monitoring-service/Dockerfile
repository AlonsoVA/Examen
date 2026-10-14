FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 appuser
COPY --from=build /app/target/app.jar app.jar
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=10 \
    CMD curl -fsS http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
