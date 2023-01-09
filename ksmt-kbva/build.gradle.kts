import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.ksmt.ksmt-base")
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(project(":ksmt-core"))
    implementation("com.github.UnitTestBot.kosat:kosat:main-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.2-eap13")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += listOf("-Xmx12g")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}
