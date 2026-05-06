#GCP 활용
# 1단계: 빌드 스테이지
FROM gradle:9-jdk17 AS build
WORKDIR /app

# 설정 파일 먼저 복사 (캐싱 활용으로 빌드 속도 향상)
COPY build.gradle settings.gradle ./
RUN gradle build -x test --no-daemon || return 0

# 소스 코드 복사 및 실제 빌드
COPY src ./src
RUN gradle clean bootJar -x test --no-daemon

# 2단계: 실행 스테이지 (용량이 작은 Alpine JRE 사용)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 4. 서버 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]