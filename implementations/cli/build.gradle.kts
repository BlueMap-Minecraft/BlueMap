plugins {
	java
	`java-library`
	id("com.diffplug.spotless") version "6.1.2"
	id ("com.github.node-gradle.node") version "3.0.1"
	id ("com.github.johnrengelman.shadow") version "8.1.1"
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
	api ("de.bluecolored.bluemap:BlueMapCommon")

	implementation ("commons-cli:commons-cli:1.5.0")

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


tasks.jar {
	manifest {
		attributes (
			"Main-Class" to "de.bluecolored.bluemap.cli.BlueMapCLI"
		)
	}
}

tasks.shadowJar {
	destinationDirectory.set(file("../../build/release"))
	archiveFileName.set("BlueMap-${project.version}-${project.name}.jar")
}

tasks.register("release") {
	dependsOn(tasks.shadowJar)
}

tasks.register("publish") {

}
