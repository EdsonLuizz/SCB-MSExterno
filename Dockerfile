# --- build (Maven) ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests=true clean package

# --- runtime (JRE) ---
FROM eclipse-temurin-17-jre
WORKDIR /app
COPY --from=build /app/target/externo-ms-0.0.1-SNAPSHOT.jar app.jar
CMD ["sh","-c","java -Dserver.port=${PORT} -jar app.jar"]
