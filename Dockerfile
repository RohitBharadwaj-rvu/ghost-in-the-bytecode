# Stage 1: Build the application (frontend is automatically copied into the JAR via maven-resources-plugin)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -f backend/pom.xml clean package -DskipTests

# Stage 2: Create the lightweight production image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add a non-root user for security best practices (DevOps Standard)
RUN addgroup -S ghost && adduser -S ghost -G ghost
USER ghost:ghost

# Copy the built JAR from the builder stage
COPY --from=build /app/backend/api/target/api-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
