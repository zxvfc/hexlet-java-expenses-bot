# Use an official Maven image to build the project, with Java 21
FROM maven:3.8.5-openjdk-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file first, so dependencies can be downloaded
COPY pom.xml .

# Download dependencies (if any), to take advantage of Docker layer caching
RUN mvn dependency:go-offline -B

# Copy the rest of the project files (source code)
COPY src ./src

# Build the project (creates the JAR file)
RUN mvn clean package -DskipTests

# Use a smaller image with Java 21 to run the app
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory for the runtime
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/myapp.jar ./myapp.jar

# Expose any ports if needed (e.g., 8080 for a web application)
EXPOSE 8080

# Set the default command to run your app
CMD ["java", "-jar", "myapp.jar"]
