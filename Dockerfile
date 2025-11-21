# Builder Stage
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace/app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon

RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# Runner Stage
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd -r spring && useradd --no-create-home -r -s /bin/false -g spring springuser

# 계층형 JAR의 각 레이어를 별도로 복사
COPY --from=builder --chown=springuser:spring /workspace/app/dependencies/ ./
COPY --from=builder --chown=springuser:spring /workspace/app/spring-boot-loader/ ./
COPY --from=builder --chown=springuser:spring /workspace/app/snapshot-dependencies/ ./
COPY --from=builder --chown=springuser:spring /workspace/app/application/ ./

USER springuser

#TODO: JVM 옵션 추가 고려, 초기 힙, 최소 힙 크기 등. (현재는 MaxRAMPercentage만 설정)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
