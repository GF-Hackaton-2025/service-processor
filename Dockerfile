FROM maven:eclipse-temurin AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/service-processor-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]