# Builder Stage
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace/app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src

RUN ./gradlew build --no-daemon -x test

# Runner Stage
FROM eclipse-temurin:17-jre

WORKDIR /app
RUN groupadd -r spring && useradd --no-create-home -r -s /bin/false -g spring springuser

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

RUN chown springuser:spring /app/app.jar

USER springuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
