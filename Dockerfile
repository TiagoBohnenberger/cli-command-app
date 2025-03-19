FROM scratch AS cp_api
WORKDIR /app
COPY api.jar api.jar

FROM maven:3.9.9-eclipse-temurin-21-alpine AS resolve_dependencies
WORKDIR /
COPY pom.xml .
RUN ["mvn", "dependency:resolve", "dependency:resolve-plugins"]

FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /
COPY --from=resolve_dependencies /root/.m2/ /root/.m2/
WORKDIR /app
COPY --from=resolve_dependencies /pom.xml .
COPY /src /app/src
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash curl

WORKDIR /app
COPY --from=build /app/target/cli-command-app-*shaded.jar cli-app.jar
COPY --from=cp_api /app/api.jar api.jar
ENTRYPOINT ["bash", "-c", " \
  java -jar /app/api.jar > /dev/null 2>&1 & \
  attempts=0; \
  while true; do \
    # Tenta acessar a API e captura o código de status HTTP \
    http_status=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080); \
    # Se o código HTTP for 404 ou o erro curl for 22, continua sem falhar \
    if [ \"$http_status\" -eq 404 ]; then \
        break; \
    fi; \
    attempts=$((attempts+1)); \
    if [ $attempts -ge 6 ]; then \
        echo \"API não iniciou após '$attempts' tentativa(s). Encerrando...\"; \
        exit 1; \
    fi; \
    sleep 1; \
  done; \
  exec java -Djakarta.enterprise.inject.scan.implicit=true -jar /app/cli-app.jar \
"]