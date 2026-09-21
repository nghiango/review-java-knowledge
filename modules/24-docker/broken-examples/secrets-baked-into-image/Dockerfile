FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Build arguments for database and private repo access
ARG GITHUB_API_TOKEN
ARG DB_PASSWORD

# Set environment variables for application runtime
ENV DB_USER="app_admin"
ENV DB_PASSWORD="${DB_PASSWORD}"
ENV GITHUB_TOKEN="${GITHUB_API_TOKEN}"

COPY app.jar .

# Test secret access and clone internal dependencies
RUN echo "Configuring private access with token: ${GITHUB_TOKEN:0:4}****"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
