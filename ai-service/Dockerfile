FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8087 9097
ENTRYPOINT ["java", "-jar", "app.jar"]
