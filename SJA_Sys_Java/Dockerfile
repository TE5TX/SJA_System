FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY settings-docker.xml /root/.m2/settings.xml
RUN mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system sja \
    && useradd --system --gid sja --home-dir /app --shell /usr/sbin/nologin sja \
    && mkdir -p /app/uploads/avatars \
    && chown -R sja:sja /app
COPY --from=build --chown=sja:sja /build/target/sja-system-1.0.0.jar /app/app.jar
USER sja
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
