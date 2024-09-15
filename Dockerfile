FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/release/*-cli.jar cli.jar

ENTRYPOINT [ "java", "-jar", "cli.jar" ]
