FROM amazoncorretto:21-alpine
WORKDIR /app
COPY build/libs/diveon-backend.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
