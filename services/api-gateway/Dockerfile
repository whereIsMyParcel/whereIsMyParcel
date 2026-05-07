FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY services ./services

RUN chmod +x gradlew
RUN ./gradlew :services:api-gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/services/api-gateway/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
