plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    `java-library`
    `maven-publish`
}

// Disable bootJar for library, enable regular jar
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}

dependencies {
    // Spring Boot (provided - consumer will have these)
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // Rate Limiting with Bucket4j
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    
    // Jackson for JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Configuration processing
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.leapfinance.infopulse"
            artifactId = "tracking-client"
            version = project.version.toString()
        }
    }
}
