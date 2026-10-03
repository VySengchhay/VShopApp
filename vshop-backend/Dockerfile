# Builds and runs the API in Docker:  docker compose up -d --build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/vshop-backend-*[0-9].jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
