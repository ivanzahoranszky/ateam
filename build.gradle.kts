plugins {
    kotlin("jvm") version "1.9.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val akkaVersion = "2.8.5"

    implementation("com.typesafe.akka:akka-stream_2.13:$akkaVersion")
    implementation("com.google.code.gson:gson:2.8.8")

    testImplementation("com.typesafe.akka:akka-testkit_2.13:$akkaVersion")
    testImplementation(platform("io.cucumber:cucumber-bom:7.14.0"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}