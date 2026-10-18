FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
RUN addgroup -S app && adduser -S app -G app && chown app:app /app/app.jar
USER app
EXPOSE 8084
HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
