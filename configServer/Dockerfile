FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY config-server/build.gradle config-server/build.gradle
COPY config-server/src config-server/src

RUN chmod +x ./gradlew
RUN ./gradlew :config-server:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV XDG_CONFIG_HOME=/tmp

COPY --from=build /app/config-server/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
