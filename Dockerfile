# Fallback when Railway Root Directory is not set to backend/
# Prefer: Service Settings → Source → Root Directory = backend
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src backend/src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=build /build/target/ai-resume-tailor-*.jar /app/app.jar
USER appuser
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
