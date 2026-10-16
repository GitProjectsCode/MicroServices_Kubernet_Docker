# Stage: Lightweight, hardened JRE runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="E-Commerce Lab DevOps"
LABEL service="product-service"

# Create a non-privileged user and group for security compliance
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the pre-packaged Spring Boot fat JAR
ARG JAR_FILE=target/product-service-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Ensure the non-root user owns the application files
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
