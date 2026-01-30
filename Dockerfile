FROM openjdk:17-jdk-slim AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r scholar && useradd -r -g scholar scholar

# Create storage directory
RUN mkdir -p storage/cvs && chown -R scholar:scholar storage/cvs

# Copy JAR from build stage
COPY --from=build /app/target/scholar-backend-1.0.0.jar app.jar

# Change ownership
RUN chown scholar:scholar app.jar

# Switch to non-root user
USER scholar

# Expose port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9090/api/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
