# Used when Railway Root Directory is the REPO ROOT (not recommended).
# Prefer: Settings → Source → Root Directory = backend  (uses backend/Dockerfile).
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends gosu \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 appuser
COPY --from=build /build/target/ai-resume-tailor-*.jar /app/app.jar
COPY backend/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN mkdir -p /app/data && chmod +x /app/docker-entrypoint.sh && chown -R appuser:appuser /app/data /app/docker-entrypoint.sh
ENV JAVA_TOOL_OPTIONS="-Xmx768m -XX:+UseContainerSupport"
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
