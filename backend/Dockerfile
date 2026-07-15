# Build from repo root: docker build -f backend/Dockerfile backend
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=build /build/target/ai-resume-tailor-*.jar /app/app.jar
USER appuser
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
