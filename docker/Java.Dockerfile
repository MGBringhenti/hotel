FROM eclipse-temurin:25-jdk AS chaves
WORKDIR /setup
COPY docker/GerarChaves.java .
CMD ["sh", "-c", "java GerarChaves.java && chown -R 10001:10001 /keys/private /keys/public"]

FROM maven:3.9-eclipse-temurin-25 AS build
ARG SERVICE
WORKDIR /build
COPY back-end/${SERVICE}/pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp dependency:go-offline
COPY back-end/${SERVICE}/src src
COPY docker/Healthcheck.java /tmp/Healthcheck.java
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -Dmaven.test.skip=true package &&     cp target/*-SNAPSHOT.jar /app.jar && javac -d /tmp /tmp/Healthcheck.java

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /app.jar app.jar
COPY --from=build /tmp/Healthcheck.class Healthcheck.class
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

