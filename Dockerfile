# Use an official Java 17 runtime as a base image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the build files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Build the app
# Using 'clean package' will compile, test, and package your app
RUN ./mvnw clean package -DskipTests

# Your 'Start Command' from before
# Make sure this jar name matches your pom.xml artifactId
CMD ["java", "-jar", "target/private-media-0.0.1-SNAPSHOT.jar"]