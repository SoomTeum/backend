# 빌드 환경: Gradle 빌드용 stage
FROM gradle:8.4.0-jdk17-alpine AS build

WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .

RUN gradle build -x test

# 실행 환경: 최종 경량 이미지
FROM openjdk:17-jdk-alpine

WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
