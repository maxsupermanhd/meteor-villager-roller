import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = "${libs.versions.mod.version.get()}+mc${libs.versions.minecraft.get()}-${getVersionMetadata()}"
    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    // Meteor
    implementation(libs.meteor.client)
    implementation(libs.orbit)
}

fun toMinecraftCompat(version: String): String {
    val match = Regex("""^(\d{2})\.([1-9]\d*)(?:\.([1-9]\d*))?$""")
        .matchEntire(version)
        ?: error("Invalid Minecraft version format: $version. Expected YY.D or YY.D.H")

    val (year, drop, _) = match.destructured
    return "~$year.$drop"
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to toMinecraftCompat(libs.versions.minecraft.get()),
            "jdk_version" to libs.versions.jdk.get(),
        )

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        val licenseSuffix = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${licenseSuffix}" }
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }
}

configurations.implementation {
    exclude("club.minnced")// fuck you
}

fun getVersionMetadata(): String {
    val buildId = System.getenv("GITHUB_RUN_NUMBER")
    if (buildId != null) {
        return "build.$buildId"
    }

    return try {
        val headHash = executeGitCommand("git", "rev-parse", "--short", "HEAD").trim()

        val status = executeGitCommand("git", "status", "--porcelain").trim()
        val isClean = status.isEmpty()

        val id = if (isClean) headHash else "$headHash-dirty"
        "rev.$id"
    } catch (e: Exception) {
        logger.warn("Unable to determine Git metadata: ${e.message}")
        "unknown"
    }
}

fun executeGitCommand(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .directory(project.rootDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val finished = process.waitFor(10, TimeUnit.SECONDS)
    if (!finished) {
        process.destroyForcibly()
        throw RuntimeException("Git command timed out: ${command.joinToString(" ")}")
    }

    if (process.exitValue() != 0) {
        val err = process.errorStream.bufferedReader().readText()
        throw RuntimeException("Git command failed: $err")
    }

    return process.inputStream.bufferedReader().readText()
}
