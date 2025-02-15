plugins {
    kotlin("plugin.serialization") version "2.1.10" apply false
    kotlin("multiplatform") version "2.1.10" apply false
    id("com.android.library") version "8.7.0" apply false
//    id("org.jetbrains.kotlin.android") version "2.1.10"
    id("cl.franciscosolis.sonatype-central-upload") version "1.0.3" apply false
    id("maven-publish")
    jacoco
}

group = "com.github.milkcocoa0902"
version = "0.2.3"

// tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        freeCompilerArgs = listOf("-Xjsr305=strict")
//        jvmTarget = "11"
//    }
// }

repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.1.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

// tasks.check {
//    dependsOn(ktlintCheck)
// }

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}