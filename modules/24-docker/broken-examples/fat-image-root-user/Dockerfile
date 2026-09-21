FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy full repository and build with Gradle inside container
COPY . .

RUN ./gradlew build -x test

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "build/libs/payment-service-1.0.0.jar"]
