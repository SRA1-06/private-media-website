# Use an official Java 17 runtime as a base image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the build files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# --- THIS IS THE FIX ---
# Copy all your source code so Maven can build it
COPY src ./src

# Build the app
RUN ./mvnw clean package -DskipTests

# Your 'Start Command'
# Using the project name 'private-media' from your error log
CMD ["java", "-jar", "target/private-media-0.0.1-SNAPSHOT.jar"]