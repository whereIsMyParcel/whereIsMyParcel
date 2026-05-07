FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY common common
COPY services services

RUN chmod +x ./gradlew
RUN ./gradlew :services:config-server:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV XDG_CONFIG_HOME=/tmp

COPY --from=build /app/services/config-server/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
