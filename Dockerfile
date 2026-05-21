FROM docker.io/library/eclipse-temurin:21-jdk-alpine@sha256:cafcfad1d9d3b6e7dd983fa367f085ca1c846ce792da59bcb420ac4424296d56 AS builder

WORKDIR /src/tk-adpro

# Make Gradle downloads more resilient on flaky networks.
ENV GRADLE_OPTS="-Dorg.gradle.wrapper.timeout=600000 -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000 -Dorg.gradle.internal.repository.max.retries=5"

# Copy Gradle wrapper and config first (better layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.lockfile ./

RUN chmod +x gradlew

# Download wrapper and resolve runtime dependencies in a cached layer.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --stacktrace -q dependencies --configuration runtimeClasspath

# Copy application source
COPY src src

# Build jar
RUN --mount=type=cache,target=/root/.gradle \
    sh -ec 'for i in 1 2 3; do \
      ./gradlew bootJar --no-daemon -x test && exit 0; \
      echo "Gradle build failed (attempt ${i}/3), retrying in 10s..."; \
      sleep 10; \
    done; \
    echo "Gradle build failed after 3 attempts"; \
    exit 1'


FROM docker.io/library/eclipse-temurin:21-jre-alpine@sha256:4e9ab608d97796571b1d5bbcd1c9f430a89a5f03fe5aa6c093888ceb6756c502 AS runner

ARG USER_NAME=adpro-a11
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

RUN addgroup -g ${USER_GID} ${USER_NAME} \
    && adduser -h /opt/tk-adpro -D -u ${USER_UID} -G ${USER_NAME} ${USER_NAME}

USER ${USER_NAME}
WORKDIR /opt/tk-adpro

COPY --from=builder --chown=${USER_UID}:${USER_GID} /src/tk-adpro/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
