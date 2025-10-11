/*
    Locally installs the mod to a folder of your choice after building.

    Instructions for use:

    Duplicate gradle.properties and delete all the text in it.
    Then rename the file to gradle-local.properties.

    Then, add this line to it:
    modsDir=/path/to/your/mods

    Obviously, replace /path/to/your/mods with the actual path to your mods folder.
 */

import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.util.Properties

val localPropsFile = file("gradle-local.properties")
if (localPropsFile.exists()) {
    val props = Properties()
    localPropsFile.inputStream().use { props.load(it) }
    props.forEach { k, v ->
        val key = k.toString()
        if (project.findProperty(key) == null) {
            project.extensions.extraProperties.set(key, v)
        }
    }
}

val projectName: String by project
val projectVersion: String by project

val modsDirProp = (project.findProperty("modsDir") as String?)?.trim()
val configuredModsDir = modsDirProp?.takeIf { it.isNotEmpty() }?.let { File(it) }

val jarTask = tasks.named<Jar>("jar")
val releaseJar = layout.buildDirectory.file("libs/${projectName}-${projectVersion}.jar")

// Copy the remapped release jar (non -dev) into the mods folder if it exists
val installMod = tasks.register<Copy>("installMod") {
    group = "build"
    description = "Copy the built mod jar into the configured mods folder (local)"
    dependsOn(jarTask)
    dependsOn("remapJar")
    from(releaseJar)
    val target = configuredModsDir
    onlyIf {
        when {
            target == null -> {
                println("Skipping installMod: modsDir not set in gradle-local.properties.")
                false
            }
            !target.exists() -> {
                println("Skipping installMod: modsDir path does not exist: $target")
                false
            }
            else -> true
        }
    }
    into(target ?: layout.buildDirectory.dir("noop"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    doFirst {
        val targetDir = target ?: return@doFirst
        val jarFile = releaseJar.get().asFile
        val destinationFile = targetDir.resolve(jarFile.name)
        if (destinationFile.exists() && !destinationFile.delete()) {
            throw GradleException("Failed to replace existing mod at $destinationFile")
        }

        println("Installing ${jarFile.name} to: $targetDir")
    }
}

// Automatically run after build when this local file is present
tasks.named("build") {
    finalizedBy(installMod)
}