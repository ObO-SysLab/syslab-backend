FROM amazoncorretto:21-alpine
RUN apk add --no-cache nodejs
WORKDIR /app
COPY build/libs/diveon-backend.jar app.jar
COPY obo-runtime ./obo-runtime
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
