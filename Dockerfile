FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV BLUEMAP_COMMAND="docker run --rm -it ghcr.io/bluemap-minecraft/bluemap"

COPY build/release/*-cli.jar cli.jar

ENTRYPOINT [ "java", "-jar", "cli.jar" ]
