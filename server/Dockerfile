FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY ./gradlew .
COPY ./gradle ./gradle
COPY ./settings.gradle* .
COPY ./gradle.properties* .

RUN ./gradlew --version --no-daemon

COPY ./build.gradle* .

RUN ./gradlew dependencies --no-daemon || true
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
