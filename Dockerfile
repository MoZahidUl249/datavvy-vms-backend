# Multi-stage build: dependencies are cached in their own layer.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY vms-platform/pom.xml vms-platform/
COPY vms-auth/pom.xml vms-auth/
COPY vms-catalog/pom.xml vms-catalog/
COPY vms-vehicle/pom.xml vms-vehicle/
COPY vms-document/pom.xml vms-document/
COPY vms-maintenance/pom.xml vms-maintenance/
COPY vms-driver/pom.xml vms-driver/
COPY vms-dashboard/pom.xml vms-dashboard/
COPY vms-app/pom.xml vms-app/
RUN mvn -B -q dependency:go-offline
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 vms
COPY --from=build /src/vms-app/target/vms-app.jar app.jar
USER vms
# JVM flags live in JAVA_TOOL_OPTIONS so a host can override them (render.yaml
# trims them for a 512 MB instance). The app listens on $PORT (default 8080).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
