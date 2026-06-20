# Stage 1: Build da aplicacao
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /code

COPY pom.xml /code/
RUN mvn -B -q dependency:go-offline

COPY src /code/src
RUN mvn -B -q package -DskipTests

# Stage 2: Runtime leve
FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:1.18
WORKDIR /deployments

COPY --from=build /code/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /code/target/quarkus-app/*.jar /deployments/
COPY --from=build /code/target/quarkus-app/app/ /deployments/app/
COPY --from=build /code/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
