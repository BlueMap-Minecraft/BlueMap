plugins {
	java
	`java-library`
	id("com.diffplug.spotless") version "6.1.2"
	id ("com.github.node-gradle.node") version "3.0.1"
	id ("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.bluecolored.bluemap.cli"
version = System.getProperty("bluemap.version") ?: "?" // set by BlueMapCore

val javaTarget = 11
java {
	sourceCompatibility = JavaVersion.toVersion(javaTarget)
	targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
	mavenCentral()
	maven {
		setUrl("https://libraries.minecraft.net")
	}
	maven {
		setUrl("https://jitpack.io")
	}
}

dependencies {
	api ("de.bluecolored.bluemap.common:BlueMapCommon")

	@Suppress("GradlePackageUpdate")
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

	//relocate ("com.flowpowered.math", "de.bluecolored.shadow.flowpowered.math") //DON"T relocate this, because the API depends on it
	relocate ("com.google", "de.bluecolored.shadow.google")
	relocate ("com.typesafe", "de.bluecolored.shadow.typesafe")
	relocate ("net.querz.nbt", "de.bluecolored.shadow.querz.nbt")
	relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
	relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.benmanes.caffeine")
	relocate ("org.aopalliance", "de.bluecolored.shadow.aopalliance")
	relocate ("javax.inject", "de.bluecolored.shadow.javax.inject")
	relocate ("javax.annotation", "de.bluecolored.shadow.javax.annotation")
	relocate ("com.mojang.brigadier", "de.bluecolored.shadow.mojang.brigadier")
	relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
	relocate ("org.codehaus", "de.bluecolored.shadow.codehaus")
	relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")
	relocate ("io.airlift", "de.bluecolored.shadow.airlift")
	relocate ("org.apache.commons", "de.bluecolored.shadow.apache.commons")
}

tasks.register("release") {
	dependsOn(tasks.shadowJar)
}

tasks.register("publish") {

}
