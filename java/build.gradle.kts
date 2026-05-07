import org.gradle.api.tasks.JavaExec

plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.railsinfra:rails-java:0.0.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("sample.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    if (findProperty("railsInsecureSsl")?.toString().equals("true", ignoreCase = true)) {
        systemProperty("rails.insecure.ssl", "true")
    }
}
