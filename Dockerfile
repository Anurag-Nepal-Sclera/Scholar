###FROM openjdk:17-jdk-slim AS build
##FROM maven:3.9.6-eclipse-temurin-17 AS build
##WORKDIR /app
##
### Copy Maven wrapper and pom.xml
##COPY .mvn/ .mvn
##COPY mvnw pom.xml ./
##
### Download dependencies
##RUN ./mvnw dependency:go-offline -B
##
### Copy source code
##COPY src ./src
##
### Build application
##RUN ./mvnw clean package -DskipTests
##
### Runtime stage
##FROM openjdk:17-jdk-slim
##
##WORKDIR /app
##
### Create non-root user
##RUN groupadd -r scholar && useradd -r -g scholar scholar
##
### Create storage directory
##RUN mkdir -p storage/cvs && chown -R scholar:scholar storage/cvs
##
### Copy JAR from build stage
##COPY --from=build /app/target/scholar-backend-1.0.0.jar app.jar
##
### Change ownership
##RUN chown scholar:scholar app.jar
##
### Switch to non-root user
##USER scholar
##
### Expose port
##EXPOSE 9090
##
### Health check
##HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
##  CMD curl -f http://localhost:9090/api/actuator/health || exit 1
##
### Run application
##ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
#
## Build stage
#FROM maven:3.9.6-eclipse-temurin-17 AS build
#WORKDIR /app
#
#COPY pom.xml ./
#RUN mvn dependency:go-offline -B
#
#COPY src ./src
#RUN mvn clean package -DskipTests -B
#
## Runtime stage
#FROM eclipse-temurin:17-jre-jammy
#
#WORKDIR /app
#
## Install curl for healthcheck
#RUN apt-get update && apt-get install -y --no-cache curl \
#    && rm -rf /var/lib/apt/lists/*
#
## Create non-root user
#RUN groupadd -r scholar && useradd -r -g scholar scholar
#
## Create storage directory with correct permissions BEFORE switching user
#RUN mkdir -p /app/storage/cvs && chown -R scholar:scholar /app
#
## Copy JAR from build stage
#COPY --from=build /app/target/scholar-backend-1.0.0.jar app.jar
#
## Switch to non-root user
#USER scholar
#
#EXPOSE 9090
#
#HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#  CMD curl -f http://localhost:9090/api/actuator/health || exit 1
#
#ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-cache curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r scholar && useradd -r -g scholar scholar

# Create storage directory with correct permissions BEFORE switching user
RUN mkdir -p /app/storage/cvs && chown -R scholar:scholar /app

# Copy JAR from build stage
COPY --from=build /app/target/scholar-backend-1.0.0.jar app.jar

# Switch to non-root user
USER scholar

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9090/api/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]