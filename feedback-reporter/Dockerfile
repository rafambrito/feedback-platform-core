# Stage 1: Build da aplicacao
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY shared /workspace/shared
COPY feedback-collector /workspace/feedback-collector
COPY feedback-notifier /workspace/feedback-notifier
COPY feedback-reporter /workspace/feedback-reporter

RUN mvn -pl feedback-reporter -am -B -q dependency:go-offline
RUN mvn -pl feedback-reporter -am -B -q package -DskipTests

# Stage 2: Runtime leve
FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:1.18
WORKDIR /deployments

COPY --from=build /workspace/feedback-reporter/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /workspace/feedback-reporter/target/quarkus-app/*.jar /deployments/
COPY --from=build /workspace/feedback-reporter/target/quarkus-app/app/ /deployments/app/
COPY --from=build /workspace/feedback-reporter/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
