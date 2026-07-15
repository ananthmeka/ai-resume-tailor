# Railway / Docker — uses Maven from image (no mvnw required on GitHub)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -r -u 10001 appuser
COPY --from=build /build/target/ai-resume-tailor-*.jar /app/app.jar
USER appuser
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Xmx768m -XX:+UseContainerSupport"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
