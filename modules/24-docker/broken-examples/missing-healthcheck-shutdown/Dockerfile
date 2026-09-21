FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY app.jar .

EXPOSE 8080

# Shell form entrypoint: invokes /bin/sh -c "java -jar app.jar"
ENTRYPOINT java -jar app.jar
