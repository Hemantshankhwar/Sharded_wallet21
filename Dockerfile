# syntax=docker/dockerfile:1

# Stage 1: build the fat jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy build files first so dependency downloads are cached
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" app.jar

# Stage 2: split the jar into layers for better image caching
FROM eclipse-temurin:21-jre AS extract
WORKDIR /extract
COPY --from=build /workspace/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# Stage 3: minimal runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
