# Use a lightweight Java 21 environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the compiled JAR from the target folder
COPY target/*.jar app.jar
# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]