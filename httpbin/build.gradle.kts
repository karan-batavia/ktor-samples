plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:2.3.7"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.bkbn:kompendium-core:3.14.4")
}

kotlin {
    jvmToolchain(17)
}

//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(17))
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//}

application {
    mainClass.set("io.ktor.samples.httpbin.ApplicationKt")
}
