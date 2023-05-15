FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY build/release/*-cli.jar cli.jar

ENTRYPOINT [ "java", "-jar", "cli.jar" ]
