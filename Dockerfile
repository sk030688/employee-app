# ─── STAGE 1: BUILD ───────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ─── STAGE 2: RUN ─────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 9090
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q -O- http://localhost:9090/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
