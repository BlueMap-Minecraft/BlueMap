import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.Options
import net.fabricmc.loom.task.RemapJarTask

plugins {
	java
	`java-library`
	id("com.diffplug.spotless") version "6.1.2"
	id ("com.github.node-gradle.node") version "3.0.1"
	id ("com.github.johnrengelman.shadow") version "7.1.2"
	id ("fabric-loom") version "0.12-SNAPSHOT"
	id ("com.modrinth.minotaur") version "2.+"
	id ("com.matthewprenger.cursegradle") version "1.4.0"
}

group = "de.bluecolored.bluemap.fabric"
version = System.getProperty("bluemap.version") ?: "?" // set by BlueMapCore

val javaTarget = 16
java {
	sourceCompatibility = JavaVersion.toVersion(javaTarget)
	targetCompatibility = JavaVersion.toVersion(javaTarget)

	withSourcesJar()
}

repositories {
	mavenCentral()
	maven {
		setUrl("https://libraries.minecraft.net")
	}
	maven {
		setUrl("https://jitpack.io")
	}
	maven {
		setUrl("https://maven.fabricmc.net/")
	}
	maven {
		setUrl("https://oss.sonatype.org/content/repositories/snapshots")
	}
}

val shadowInclude: Configuration by configurations.creating

configurations {
	implementation.get().extendsFrom(shadowInclude)
}

dependencies {
	shadowInclude ("de.bluecolored.bluemap.common:BlueMapCommon") {
		//exclude dependencies provided by fabric
		exclude (group = "com.google.guava", module = "guava")
		exclude (group = "com.google.code.gson", module = "gson")
		exclude (group = "org.apache.commons", module = "commons-lang3")
		exclude (group = "commons-io", module = "commons-io")
		exclude (group = "com.mojang", module = "brigadier")
	}

	minecraft ("com.mojang:minecraft:1.17")
	mappings ("net.fabricmc:yarn:1.17+build.1:v2")
	modImplementation ("net.fabricmc:fabric-loader:0.11.3")
	modImplementation ("net.fabricmc.fabric-api:fabric-api:0.34.9+1.17")
	modImplementation("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")

	testImplementation ("org.junit.jupiter:junit-jupiter:5.8.2")
	testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

spotless {
	java {
		target ("src/*/java/**/*.java")

		licenseHeaderFile("../../HEADER")
		indentWithSpaces()
		trimTrailingWhitespace()
	}
}

tasks.withType(JavaCompile::class).configureEach {
	options.apply {
		encoding = "utf-8"
	}
}

tasks.withType(AbstractArchiveTask::class).configureEach {
	isReproducibleFileOrder = true
	isPreserveFileTimestamps = false
}

tasks.test {
	useJUnitPlatform()
}

tasks.processResources {
	inputs.property ("version", project.version)

	filesMatching("fabric.mod.json") {
		expand ("version" to project.version)
	}
}

tasks.shadowJar {
	configurations = listOf(shadowInclude)

	//relocate ("com.flowpowered.math", "de.bluecolored.shadow.flowpowered.math") //DON"T relocate this, because the API depends on it
	relocate ("com.typesafe.config", "de.bluecolored.shadow.typesafe.config")
	relocate ("net.querz.nbt", "de.bluecolored.shadow.querz.nbt")
	relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
	relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.benmanes.caffeine")
	relocate ("org.aopalliance", "de.bluecolored.shadow.aopalliance")
	relocate ("javax.inject", "de.bluecolored.shadow.javax.inject")
	relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
	relocate ("org.codehaus", "de.bluecolored.shadow.codehaus")
	relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")

	relocate ("com.google.errorprone", "de.bluecolored.shadow.google.errorprone")
	relocate ("com.google.inject", "de.bluecolored.shadow.google.inject")

	relocate ("org.apache.commons.dbcp2", "de.bluecolored.shadow.apache.commons.dbcp2")
	relocate ("org.apache.commons.logging", "de.bluecolored.shadow.apache.commons.logging")
	relocate ("org.apache.commons.pool2", "de.bluecolored.shadow.apache.commons.pool2")
}

tasks.register("remappedShadowJar", type = RemapJarTask::class) {
	destinationDirectory.set(file("../../build/release"))
	archiveFileName.set("BlueMap-${project.version}-${project.name}.jar")
	dependsOn (tasks.shadowJar)
	inputFile.set(tasks.shadowJar.get().archiveFile)
	addNestedDependencies.set(true)
}

tasks.register("release") {
	dependsOn("remappedShadowJar")
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set("swbUV1cr")
	versionNumber.set("${project.version}-${project.name}")
	changelog.set("Releasenotes and Changelog:\nhttps://github.com/BlueMap-Minecraft/BlueMap/releases/tag/v${project.version}")
	uploadFile.set(tasks.findByName("remappedShadowJar"))
	gameVersions.addAll("1.17", "1.17.1")
	dependencies {
		required.project("P7dR8mSH") // Fabric API
	}
}

curseforge {
	apiKey = System.getenv("CURSEFORGE_TOKEN") ?: ""
	project(closureOf<CurseProject> {
		id = "406463"
		changelogType = "markdown"
		changelog = "**Releasenotes and Changelog:**\n\nhttps://github.com/BlueMap-Minecraft/BlueMap/releases/tag/v${project.version}"
		releaseType = "release"

		addGameVersion("Fabric")

		addGameVersion("Java 18")
		addGameVersion("Java 17")
		addGameVersion("Java 16")

		addGameVersion("1.17")
		addGameVersion("1.17.1")

		mainArtifact(tasks.findByName("remappedShadowJar"), closureOf<CurseArtifact> {
			relations(closureOf<CurseRelation> {
				requiredDependency("fabric-api")
			})
		})
	})
	options(closureOf<Options> {
		javaVersionAutoDetect = false
		javaIntegration = false
		forgeGradleIntegration = false
	})
}

tasks.register("publish") {
	dependsOn("modrinth")
	dependsOn("curseforge")
}
