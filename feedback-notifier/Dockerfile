# Stage 1: Build da aplicacao
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /code

COPY --chown=1001:0 mvnw /code/mvnw
COPY --chown=1001:0 .mvn /code/.mvn
COPY --chown=1001:0 pom.xml /code/
RUN chmod +x /code/mvnw && /code/mvnw -B -q dependency:go-offline

COPY --chown=1001:0 src /code/src
RUN /code/mvnw -B -q package -DskipTests

# Stage 2: Runtime leve
FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:1.18
WORKDIR /deployments

COPY --from=build /code/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /code/target/quarkus-app/*.jar /deployments/
COPY --from=build /code/target/quarkus-app/app/ /deployments/app/
COPY --from=build /code/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
