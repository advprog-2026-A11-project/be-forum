plugins {
    java
    id("jacoco")
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.1.0.6387"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "forum"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

sonarqube {
    properties {
        property("sonar.projectKey", "advprog-2026-A11-project_be-forum")
        property("sonar.organization", "adpro-a-kelompok-11")
    }
}

dependencyManagement {
    dependencies {
        dependency("org.apache.tomcat.embed:tomcat-embed-core:10.1.55")
        dependency("org.apache.tomcat.embed:tomcat-embed-el:10.1.55")
        dependency("org.apache.tomcat.embed:tomcat-embed-websocket:10.1.55")

        dependency("org.springframework:spring-web:6.2.18")
        dependency("org.springframework:spring-webmvc:6.2.18")

        dependency("org.springframework.security:spring-security-core:6.5.10")
        dependency("org.springframework.security:spring-security-web:6.5.10")
        dependency("org.springframework.security:spring-security-oauth2-jose:6.5.10")

        dependency("com.fasterxml.jackson.core:jackson-core:2.21.2")

        dependency("io.netty:netty-buffer:4.1.133.Final")
        dependency("io.netty:netty-codec:4.1.133.Final")
        dependency("io.netty:netty-common:4.1.133.Final")
        dependency("io.netty:netty-handler:4.1.133.Final")
        dependency("io.netty:netty-resolver:4.1.133.Final")
        dependency("io.netty:netty-transport:4.1.133.Final")
        dependency("io.netty:netty-transport-native-unix-common:4.1.133.Final")

        dependency("io.opentelemetry:opentelemetry-api:1.62.0")
        dependency("io.opentelemetry:opentelemetry-context:1.62.0")
        dependency("io.opentelemetry:opentelemetry-extension-trace-propagators:1.62.0")
        dependency("io.opentelemetry:opentelemetry-exporter-common:1.62.0")
        dependency("io.opentelemetry:opentelemetry-exporter-otlp-common:1.62.0")
        dependency("io.opentelemetry:opentelemetry-exporter-otlp:1.62.0")
        dependency("io.opentelemetry:opentelemetry-exporter-sender-okhttp:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk-common:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk-logs:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk-metrics:1.62.0")
        dependency("io.opentelemetry:opentelemetry-sdk-trace:1.62.0")

        dependency("org.thymeleaf:thymeleaf:3.1.5.RELEASE")
        dependency("org.thymeleaf:thymeleaf-spring6:3.1.5.RELEASE")

        dependency("org.postgresql:postgresql:42.7.11")
        dependency("org.apache.commons:commons-compress:1.26.0")
        dependency("org.apache.commons:commons-lang3:3.18.0")
        dependency("org.assertj:assertj-core:3.27.7")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-registry-otlp")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.16.0-alpha")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.h2database:h2")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.2")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
    }
}
