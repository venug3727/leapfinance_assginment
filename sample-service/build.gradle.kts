plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Include the tracking client library
    implementation(project(":tracking-client"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveFileName.set("sample-service.jar")
}
