FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk update && \
    apk upgrade

COPY build/release/*-cli.jar cli.jar

ENTRYPOINT [ "java", "-jar", "cli.jar" ]
