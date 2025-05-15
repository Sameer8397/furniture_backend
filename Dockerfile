# Use a lightweight OpenJDK image
FROM eclipse-temurin:17-jdk-alpine

# Copy your built jar to the container
COPY target/furniture-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app.jar"]
