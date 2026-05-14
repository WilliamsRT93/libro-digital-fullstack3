# syntax=docker/dockerfile:1.6
# Build multi-etapa para reducir el tamanio final de la imagen
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Timeouts largos y reintentos para tolerar redes lentas y descargas pesadas
ENV MAVEN_OPTS="-Daether.connector.connectTimeout=300000 \
    -Daether.connector.requestTimeout=900000 \
    -Daether.connector.http.connectionMaxTtl=300 \
    -Daether.connector.http.retryHandler.count=10 \
    -Daether.connector.http.retryHandler.requestSentEnabled=true \
    -Dmaven.wagon.http.pool=true \
    -Dmaven.wagon.http.retryHandler.count=10 \
    -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=300"

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline || mvn -B dependency:go-offline || mvn -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# Imagen final con JRE liviano y usuario no root
FROM eclipse-temurin:21.0.5_11-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
