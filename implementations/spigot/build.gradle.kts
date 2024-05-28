plugins {
	java
	`java-library`
	id("com.diffplug.spotless") version "6.1.2"
	id ("com.github.node-gradle.node") version "3.0.1"
	id ("com.github.johnrengelman.shadow") version "7.1.2"
	id ("com.modrinth.minotaur") version "2.+"
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
	maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
		content {
			includeGroup ("org.spigotmc")
		}
	}
	maven ("https://oss.sonatype.org/content/repositories/snapshots")
	maven ("https://repo.bluecolored.de/releases")
}

dependencies {
	api ("de.bluecolored.bluemap:BlueMapCommon") {
		//exclude dependencies provided by bukkit
		exclude( group = "com.google.guava", module = "guava" )
		exclude( group = "com.google.code.gson", module = "gson" )
	}

	shadow ("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
	implementation ("org.bstats:bstats-bukkit:2.2.1")

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
	from("src/main/resources") {
		include("plugin.yml")
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
	relocate ("com.typesafe.config", "de.bluecolored.shadow.typesafe.config")
	relocate ("de.bluecolored.bluenbt", "de.bluecolored.shadow.bluenbt")
	relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
	relocate ("org.bstats", "de.bluecolored.shadow.bstats")
	relocate ("com.mojang.brigadier", "de.bluecolored.shadow.mojang.brigadier")
	relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.benmanes.caffeine")
	relocate ("org.aopalliance", "de.bluecolored.shadow.aopalliance")
	relocate ("javax.inject", "de.bluecolored.shadow.javax.inject")
	relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
	relocate ("org.codehaus", "de.bluecolored.shadow.codehaus")
	relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")
	relocate ("io.airlift", "de.bluecolored.shadow.airlift")
	relocate ("net.jpountz", "de.bluecolored.shadow.jpountz")

	relocate ("com.google.errorprone", "de.bluecolored.shadow.google.errorprone")
	relocate ("com.google.inject", "de.bluecolored.shadow.google.inject")

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
	loaders.addAll("spigot", "paper", "purpur")
	gameVersions.addAll(
		"1.16.5",
		"1.17", "1.17.1",
		"1.18", "1.18.1", "1.18.2",
		"1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
		"1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"
	)
}

tasks.register("publish") {
	dependsOn("modrinth")
}
