import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.ksmt.ksmt-base")
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    flatDir { dirs("dist") }
}

val kissatNative by configurations.creating

dependencies {
    implementation(project(":ksmt-core"))

    implementation("com.github.UnitTestBot.kosat:kosat:65d3205c17")

    api("net.java.dev.jna:jna:5.12.0")
    implementation("net.java.dev.jna:jna-platform:5.12.0")

    kissatNative("kissat", "kissat-native-linux-x86-64", "3.0.0", ext = "zip")
    kissatNative("kissat", "kissat-native-win32-x86-64", "3.0.0", ext = "zip")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.withType<ProcessResources> {
    kissatNative.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        // destination must be in format {OS}-{ARCH} according to JNA docs
        // https://github.com/java-native-access/jna/blob/master/www/GettingStarted.md
        val destination = artifact.name.removePrefix("kissat-native-")
        from(zipTree(artifact.file)) {
            into(destination)
        }
    }
}
