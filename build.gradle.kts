import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    `maven-publish`
}

group = "com.asyncsite.coreplatform"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot basics
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kafka - Core dependencies
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-clients")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic")

    // For MDC context propagation
    compileOnly("org.slf4j:slf4j-api")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

// This is a library, not a bootable application
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Sources JAR 생성
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.asyncsite.coreplatform"
            artifactId = "common-kafka"
            version = "1.0.0-SNAPSHOT"

            from(components["java"])

            // Sources JAR 포함
            artifact(sourcesJar)

            pom {
                name = "Common Kafka"
                description = "Kafka components for AsyncSite microservices"
                url = "https://github.com/AsyncSite/common-kafka"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "asyncsite"
                        name = "AsyncSite Team"
                        email = "team@asyncsite.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/AsyncSite/common-kafka.git"
                    developerConnection = "scm:git:ssh://github.com/AsyncSite/common-kafka.git"
                    url = "https://github.com/AsyncSite/common-kafka"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AsyncSite/common-kafka")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}