plugins {
    kotlin("jvm") version "1.9.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.typesafe.akka:akka-stream_2.13:2.8.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}