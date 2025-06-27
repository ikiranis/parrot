# Multi-stage build for Parrot Tunes Backend

# Build stage
FROM openjdk:17-jdk-slim as build

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p uploads/covers uploads/temp

# Expose port
EXPOSE 9999

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:9999/api/auth/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
