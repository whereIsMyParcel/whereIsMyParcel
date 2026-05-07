FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY services ./services

RUN chmod +x gradlew
RUN ./gradlew :services:eureka-server:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/services/eureka-server/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
