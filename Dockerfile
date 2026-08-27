FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
COPY src src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8001

ENTRYPOINT ["java", "-jar", "app.jar"]