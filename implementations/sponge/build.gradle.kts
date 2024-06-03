import org.spongepowered.gradle.plugin.config.PluginLoaders

plugins {
	java
	`java-library`
	id("com.diffplug.spotless") version "6.1.2"
	id ("com.github.node-gradle.node") version "3.0.1"
	id ("com.github.johnrengelman.shadow") version "8.1.1"
	id ("org.spongepowered.gradle.plugin") version "2.0.0"
	id ("com.modrinth.minotaur") version "2.+"
	id("org.spongepowered.gradle.ore") version "2.2.0"
}

group = "de.bluecolored.bluemap"
version = System.getProperty("bluemap.version") ?: "?" // set by BlueMapCore

val javaTarget = 16
java {
	sourceCompatibility = JavaVersion.toVersion(javaTarget)
	targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
	mavenCentral()
	maven ("https://libraries.minecraft.net")
	maven ("https://repo.bluecolored.de/releases")
}

dependencies {
	api ("de.bluecolored.bluemap:BlueMapCommon"){
		//exclude dependencies provided by sponge
		exclude( group = "com.google.guava", module = "guava" )
		exclude( group = "com.google.code.gson", module = "gson" )
		exclude( group = "javax.inject" )
		exclude( group = "com.google.inject" )
	}

	implementation ("org.bstats:bstats-sponge:2.2.1")

	testImplementation ("org.junit.jupiter:junit-jupiter:5.8.2")
	testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

sponge {
	apiVersion("8.2.0")
	license("MIT")
	loader {
		name(PluginLoaders.JAVA_PLAIN)
		version("1.0")
	}
	plugin("bluemap") {
		displayName("bluemap")
		entrypoint("de.bluecolored.bluemap.sponge.SpongePlugin")
		description("A 3d-map of your Minecraft worlds view-able in your browser using three.js (WebGL)")
		contributor("Blue (TBlueF, Lukas Rieger)") {
			description("Lead Developer")
		}
		dependency("spongeapi") {
			version("8.2.0")
			optional(false)
		}
	}
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
	from("src/main/resources") {
		include("META-INF/plugins.json")
		duplicatesStrategy = DuplicatesStrategy.INCLUDE

		expand (
			"version" to project.version
		)
	}
}

tasks.shadowJar {
	destinationDirectory.set(file("../../build/release"))
	archiveFileName.set("BlueMap-${project.version}-${project.name}.jar")

	//relocate ("com.flowpowered.math", "de.bluecolored.shadow.flowpowered.math") //DON"T relocate this, because the API depends on it
	relocate ("de.bluecolored.bluenbt", "de.bluecolored.shadow.bluenbt")
	relocate ("com.mojang.brigadier", "de.bluecolored.shadow.mojang.brigadier")
	relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.benmanes.caffeine")
	relocate ("com.google.errorprone", "de.bluecolored.shadow.google.errorprone")
	relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
	relocate ("org.aopalliance", "de.bluecolored.shadow.aopalliance")
	relocate ("org.bstats", "de.bluecolored.shadow.bstats")
	relocate ("com.typesafe.config", "de.bluecolored.shadow.typesafe.config")
	relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
	relocate ("org.codehaus", "de.bluecolored.shadow.codehaus")
	relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")
	relocate ("io.airlift", "de.bluecolored.shadow.airlift")
	relocate ("net.jpountz", "de.bluecolored.shadow.jpountz")

	relocate ("org.apache.commons.dbcp2", "de.bluecolored.shadow.apache.commons.dbcp2")
	relocate ("org.apache.commons.logging", "de.bluecolored.shadow.apache.commons.logging")
	relocate ("org.apache.commons.pool2", "de.bluecolored.shadow.apache.commons.pool2")
}

tasks.register("release") {
	dependsOn(tasks.shadowJar)
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set("swbUV1cr")
	versionNumber.set("${project.version}-${project.name}")
	changelog.set(file("../../release.md")
		.readText()
		.replace("{version}", project.version.toString()))
	uploadFile.set(tasks.findByName("shadowJar"))
	loaders.addAll("sponge")
	gameVersions.addAll("1.16.5")
}

tasks.register("publish") {
	dependsOn("modrinth")
	dependsOn("publishToOre")
}

oreDeployment {
	apiKey(System.getenv("ORE_TOKEN"))
	defaultPublication {
		projectId.set("bluemap")
		createForumPost.set(true)
		versionBody.set(file("../../release.md")
			.readText()
			.replace("{version}", project.version.toString()))
		publishArtifacts.setFrom(tasks.findByName("shadowJar"))
	}
}
