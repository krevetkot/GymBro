FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

ARG SERVICE

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY config-server/build.gradle config-server/
COPY eureka-server/build.gradle eureka-server/
COPY gateway/build.gradle gateway/
COPY monolith/build.gradle monolith/
COPY identity-service/build.gradle identity-service/
RUN chmod +x gradlew && ./gradlew --no-daemon :${SERVICE}:dependencies

COPY ${SERVICE}/src ${SERVICE}/src
RUN ./gradlew --no-daemon :${SERVICE}:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG SERVICE

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/${SERVICE}/build/libs/app.jar app.jar
USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
