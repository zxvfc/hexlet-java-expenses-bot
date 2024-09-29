# Stage 1: Use an official JDK 21 image to build the project
FROM eclipse-temurin:21-jdk AS build

# Install Maven manually
RUN apt-get update && apt-get install -y maven

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies (for layer caching)
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the project
COPY src ./src

# Build the project (this will create the JAR file)
RUN mvn clean package -DskipTests

# Stage 2: Use a lightweight JDK 21 image to run the app
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory for the runtime
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/myapp.jar ./myapp.jar

# Expose any necessary ports (if required)
EXPOSE 8080

# Default command to run the application
CMD ["java", "-jar", "myapp.jar"]
