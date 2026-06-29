# Stage 1: Build the JAR using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL org.opencontainers.image.description="JavadocViewerService serves and manages Javadoc documentation from Git repositories."

WORKDIR /build

# Copy pom.xml and download dependencies first (for Docker cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the full source tree and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Optional: add tini to manage signals properly
RUN apt-get update && apt-get install -y tini && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN useradd -m jvsuser
USER jvsuser

# Copy the built jar from the builder stage
COPY --from=builder /build/target/*.jar /app/jvs.jar

VOLUME ["/app/javadoc-storage", "/app/database"]

EXPOSE 8080

ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["java", "-jar", "jvs.jar"]
