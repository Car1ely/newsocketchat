FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем pom.xml и загружаем зависимости (кэширование слоев)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Копируем собранный JAR из build stage
COPY --from=build /app/target/untitled-1.0-SNAPSHOT.jar /app/app.jar

# Создаем непривилегированного пользователя
RUN useradd -m -u 1000 chatuser && \
    chown -R chatuser:chatuser /app

USER chatuser

# По умолчанию показываем help
CMD ["java", "-cp", "/app/app.jar", "org.example.Main", "help"]