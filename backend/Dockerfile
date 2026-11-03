# Etapa 1: compilar
FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# Etapa 2: ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /app/target/*.jar biohub.jar
ENTRYPOINT ["java", "-jar", "biohub.jar"]