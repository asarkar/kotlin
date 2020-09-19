import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.asarkar.kotlin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotlinxVersion: String by project
val junitVersion: String by project
dependencies {
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinxVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.majorVersion
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs = listOf("-Dkotlinx.coroutines.debug")
    testLogging.showStandardStreams = true
}