# Use an official Maven image to build the project, with OpenJDK 21
FROM maven:3.8.6-openjdk-21-slim AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project (this will generate the JAR)
RUN mvn clean package

# Use a smaller base image for the runtime
FROM openjdk:21-slim

# Set the working directory for the runtime
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/hexlet-java-tg-bot-2-1.0-SNAPSHOT.jar ./hexlet-java-tg-bot-2-1.0-SNAPSHOT.jar

# Expose any necessary ports (if required)
EXPOSE 8080

# Run the built JAR
CMD ["java", "-jar", "hexlet-java-tg-bot-2-1.0-SNAPSHOT.jar"]
