# ------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
# We wouldn't need a builder if we had a pipeline that built the artifact and pushed it to a registry

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn package -DskipTests -B \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# JVM Configuration for containers
# - UseContainerSupport: Respect container memory limits
# - MaxRAMPercentage: Use 75% of available container memory for heap
# - InitialRAMPercentage: Start with 50% to reduce startup time
# - +ExitOnOutOfMemoryError: Fail fast for K8s to restart
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

# Health check for Docker (K8s uses its own probes)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# It's faster...
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
