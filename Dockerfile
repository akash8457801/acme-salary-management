# Multi-stage build: Node builds the SPA, Maven bakes it into the jar, a slim JRE runs it.
# The result is one container, one process, one port — no reverse proxy to configure.

FROM node:22-alpine AS ui
WORKDIR /ui
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=ui /ui/dist/salary-management/browser /frontend/dist/salary-management/browser
RUN ./mvnw -q -B -DskipTests -Pbundle-ui -Dfrontend.dist=/frontend/dist/salary-management/browser package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/salary-management-1.0.0.jar app.jar
# The SQLite file lives on a volume so data survives restarts; first boot seeds 10,000 employees.
ENV SALARY_DB_PATH=/data/salary.db
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
