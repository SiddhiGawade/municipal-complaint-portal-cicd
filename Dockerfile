FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid 10001 --home-dir /nonexistent \
        --shell /usr/sbin/nologin app
COPY --from=build --chown=10001:10001 \
    /workspace/target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar \
    /app/municipal-complaint-portal.jar

ENV PORT=8085 BIND_ADDRESS=0.0.0.0
EXPOSE 8085
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/municipal-complaint-portal.jar"]
