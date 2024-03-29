import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40"
}

group = "as"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "1.2.1"
    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"
    fun ktor() = "io.ktor:ktor:$ktorVersion"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    implementation(ktor())
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("websockets"))
    implementation(ktor("client-core"))
    implementation(ktor("client-websockets"))
    implementation(ktor("client-cio"))
    implementation(ktor("client-js"))
    implementation(ktor("client-okhttp"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
