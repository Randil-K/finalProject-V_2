# Build stage — the Maven image avoids relying on mvnw, whose Windows line endings break on Linux.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/cleanup-backend-*.jar app.jar

# Runtime stage — JRE only, non-root.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 tideline \n    && mkdir -p /app/uploads && chown tideline /app/uploads
COPY --from=build /app/app.jar app.jar
USER tideline

# Production defaults: MySQL profile, and heap sized to the container rather than the host.
ENV SPRING_PROFILES_ACTIVE=mysql \
    JAVA_OPTS="-XX:MaxRAMPercentage=75" \n    UPLOADS_DIR=/app/uploads

# Diver certificates are stored here; mount a persistent volume so they survive redeploys.
VOLUME /app/uploads

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
