import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25"
    application
}

application {
    mainClass.set("sample.MainKt")
}

dependencies {
    implementation("com.rails.api:rails-kotlin:0.0.1")

    val ktor = "2.3.12"
    implementation("io.ktor:ktor-server-core-jvm:$ktor")
    implementation("io.ktor:ktor-server-cio-jvm:$ktor")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

// Forwards -PrailsInsecureSsl=true into the app JVM (Gradle daemon often lacks your shell exports).
tasks.named<JavaExec>("run") {
    if (findProperty("railsInsecureSsl")?.toString().equals("true", ignoreCase = true)) {
        systemProperty("rails.insecure.ssl", "true")
    }
}
