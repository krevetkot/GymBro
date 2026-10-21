# syntax=docker/dockerfile:1

# Сборка в образе с уже установленным Gradle той же версии, что и в wrapper: семь сервисов
# собираются параллельно, и скачивание дистрибутива Gradle в каждом из них обрывалось.
FROM gradle:8.14.3-jdk21-alpine AS build
WORKDIR /app

ARG SERVICE

COPY build.gradle settings.gradle ./
COPY config-server/build.gradle config-server/
COPY eureka-server/build.gradle eureka-server/
COPY gateway/build.gradle gateway/
COPY identity-service/build.gradle identity-service/
COPY catalog-service/build.gradle catalog-service/
COPY profile-service/build.gradle profile-service/
COPY matching-service/build.gradle matching-service/
COPY ${SERVICE}/src ${SERVICE}/src

# Кеш зависимостей общий для сборок всех сервисов и между запусками: библиотеки скачиваются один раз
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon :${SERVICE}:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG SERVICE

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/${SERVICE}/build/libs/app.jar app.jar
USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
