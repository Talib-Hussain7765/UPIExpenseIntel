FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]