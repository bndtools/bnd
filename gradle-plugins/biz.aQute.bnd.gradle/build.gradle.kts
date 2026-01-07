import groovy.lang.GroovySystem
import org.gradle.util.internal.VersionNumber
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

import java.util.*

plugins {
	groovy
	`kotlin-dsl`
	id("com.gradle.plugin-publish")
	id("dev.hargrave.addmavendescriptor")
	signing
}

interface Injected {
	@get:Inject val fs: FileSystemOperations
}

// From gradle.properties
val bnd_group: String by project
val bnd_version: String by project
val bnd_distrepo: String by project

group = bnd_group
version = bnd_version

val groovyVersion = GroovySystem.getVersion()
val isGroovy4 = VersionNumber.parse(groovyVersion).major >= 4
val spockVersion = if (isGroovy4) "2.3-groovy-4.0" else "2.3-groovy-3.0"

val javaVersion = JavaVersion.VERSION_17 // Bnd target language level
val kotlinVersion = KotlinVersion.KOTLIN_2_0 // Supported in Gradle 8

java {
	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion
	withJavadocJar()
	withSourcesJar()
}

val maven_repo_local: String? by rootProject.extra
repositories {
	mavenLocal {
		maven_repo_local?.let {
			url = uri(it)
		}
		metadataSources {
			mavenPom()
			artifact()
		}
	}
	mavenCentral()
}

// SourceSet for Kotlin DSL code so that it can be built after the main SourceSet
val dsl: SourceSet by sourceSets.creating
sourceSets {
	dsl.apply {
		compileClasspath += main.get().output
		runtimeClasspath += main.get().output
	}
	test {
		compileClasspath += dsl.output
		runtimeClasspath += dsl.output
	}
}

configurations {
	dsl.compileOnlyConfigurationName {
		extendsFrom(compileOnly.get())
	}
	dsl.implementationConfigurationName {
		extendsFrom(implementation.get())
	}
	dsl.runtimeOnlyConfigurationName {
		extendsFrom(runtimeOnly.get())
	}
}

// Dependencies
dependencies {
	implementation("biz.aQute.bnd:biz.aQute.bnd.util:${version}")
	implementation("biz.aQute.bnd:biz.aQute.bndlib:${version}")
	implementation("biz.aQute.bnd:biz.aQute.repository:${version}")
	implementation("biz.aQute.bnd:biz.aQute.resolve:${version}")
	runtimeOnly("biz.aQute.bnd:biz.aQute.bnd.embedded-repo:${version}")
	// keep in sync with cnf/junit.bnd e.g. 'junit.jupiter.version'
	testImplementation(enforcedPlatform("org.junit:junit-bom:6.0.2"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.spockframework:spock-core:${spockVersion}")
}

// Gradle plugin descriptions
gradlePlugin {
	website.set("https://github.com/bndtools/bnd")
	vcsUrl.set("https://github.com/bndtools/bnd.git")
	plugins {
		create("Bnd") {
			id = "biz.aQute.bnd"
			implementationClass = "aQute.bnd.gradle.BndPlugin"
			displayName = "Bnd Gradle Plugin for Bnd Workspace Projects"
			description = "Gradle Plugin for developing OSGi bundles with Bnd using the Bnd Workspace build. Bnd is the premiere tool for creating OSGi bundles. This Gradle plugin is from the team that develops Bnd and is used by the Bnd team to build Bnd itself. See https://github.com/bndtools/bnd/blob/${version}/gradle-plugins/README.md for information on using in a Bnd Workspace build."
			tags.set(listOf("osgi", "bnd"))
		}
		create("BndBuilder") {
			id = "biz.aQute.bnd.builder"
			implementationClass = "aQute.bnd.gradle.BndBuilderPlugin"
			displayName = "Bnd Gradle Plugin for Gradle Projects"
			description = "Gradle Plugin for developing OSGi bundles with Bnd in a typical Gradle build. Bnd is the premiere tool for creating OSGi bundles. This Gradle plugin is from the team that develops Bnd. See https://github.com/bndtools/bnd/blob/${version}/gradle-plugins/README.md for information on using in a typical Gradle build."
			tags.set(listOf("osgi", "bnd"))
		}
		create("BndWorkspace") {
			id = "biz.aQute.bnd.workspace"
			implementationClass = "aQute.bnd.gradle.BndWorkspacePlugin"
			displayName = "Bnd Gradle Plugin for the Bnd Workspace"
			description = "Gradle Plugin for developing OSGi bundles with Bnd using the Bnd Workspace build. Bnd is the premiere tool for creating OSGi bundles. This Gradle plugin is from the team that develops Bnd and is used by the Bnd team to build Bnd itself. See https://github.com/bndtools/bnd/blob/${version}/gradle-plugins/README.md for information on using on a Bnd Workspace."
			tags.set(listOf("osgi", "bnd"))
		}
	}
}

publishing {
	repositories {
		maven {
			name = "Dist"
			url = uri(rootProject.layout.projectDirectory).resolve(bnd_distrepo)
		}
		if (System.getenv("CANONICAL").toBoolean()) {
			val releaseType = if (version.toString().endsWith("SNAPSHOT")) "snapshot" else "release"
			maven {
				name = "JFrog"
				url = uri("https://bndtools.jfrog.io/bndtools/libs-${releaseType}-local/")
				credentials {
					username = System.getenv("JFROG_USERNAME") ?: ""
					password = System.getenv("JFROG_PASSWORD") ?: ""
				}
			}
		}
	}
	publications {
		// Main plugin publication
		create<MavenPublication>("pluginMaven") {
			pom {
				name.set(artifactId)
				description.set("The Bnd Gradle plugins.")
			}
		}
		// Configure pom metadata
		withType<MavenPublication> {
			pom {
				url.set("https://bnd.bndtools.org/")
				organization {
					name.set("Bndtools")
					url.set("https://bndtools.org/")
				}
				licenses {
					license {
						name.set("(Apache-2.0 OR EPL-2.0)")
						url.set("https://opensource.org/licenses/Apache-2.0,https://opensource.org/licenses/EPL-2.0")
						distribution.set("repo")
						comments.set("This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0, or the Eclipse Public License 2.0.")
					}
				}
				scm {
					url.set("https://github.com/bndtools/bnd")
					connection.set("scm:git:https://github.com/bndtools/bnd.git")
					developerConnection.set("scm:git:git@github.com:bndtools/bnd.git")
					tag.set(version.toString())
				}
				developers {
					developer {
						id.set("bjhargrave")
						name.set("BJ Hargrave")
						email.set("bj@hargrave.dev")
						url.set("https://github.com/bjhargrave")
						organization.set("IBM")
						organizationUrl.set("https://developer.ibm.com")
						roles.set(setOf("developer"))
						timezone.set("America/New_York")
					}
				}
			}
		}
	}
}

// Signing configuration
signing {
	// Only sign if GPG_KEY_ID environment variable is present
	val gpgKeyId: String? = System.getenv("GPG_KEY_ID")
	
	isRequired = gpgKeyId != null
	
	if (isRequired) {
		// Use GPG command since key is imported into GPG keyring by ci-build.sh
		// The passphrase is handled by gpg-agent which was configured in ci-build.sh
		useGpgCmd()
		sign(publishing.publications)
	}
}

// Java compiler options
tasks.withType<JavaCompile>() {
	options.compilerArgs.add("-Xlint:deprecation")
	options.compilerArgs.add("-Xlint:unchecked")
}

tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>().configureEach {
	compilerOptions {
		jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		apiVersion = kotlinVersion
		languageVersion = kotlinVersion
	}
}

// Disable gradle module metadata
tasks.withType<GenerateModuleMetadata>().configureEach {
	enabled = false
}

// Reproducible jars
tasks.withType<AbstractArchiveTask>().configureEach {
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
}

// Reproducible javadoc
tasks.withType<Javadoc>().configureEach {
	options {
		this as StandardJavadocDocletOptions // unsafe cast
		isNoTimestamp = true
	}
}

tasks.pluginUnderTestMetadata {
	// Include dsl SourceSet
	pluginClasspath.from(dsl.output)
}

tasks.jar {
	// Include dsl SourceSet
	from(dsl.output)
}

tasks.named<Jar>("sourcesJar") {
	// Include dsl SourceSet
	from(dsl.allSource)
}

val testresourcesOutput = layout.buildDirectory.dir("testresources")

// Configure test
tasks.test {
	useJUnitPlatform()
	reports {
		junitXml.apply {
			isOutputPerTestCase = true
			mergeReruns.set(true)
		}
	}
	testLogging {
		setExceptionFormat("full")
		info {
			events("STANDARD_OUT", "STANDARD_ERROR", "STARTED", "FAILED", "PASSED", "SKIPPED")
		}
	}
	val testresourcesSource = layout.projectDirectory.dir("testresources")
	inputs.files(testresourcesSource).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("testresources")
	systemProperty("bnd_version", bnd_version)
	systemProperty("org.gradle.warning.mode", gradle.startParameter.warningMode.name.lowercase(Locale.ROOT))
	maven_repo_local?.let {
		systemProperty("maven.repo.local", it)
	}
	val injected = objects.newInstance<Injected>()
	doFirst {
		// copy test resources into build dir
		injected.fs.delete {
			delete(testresourcesOutput)
		}
		injected.fs.copy {
			from(testresourcesSource)
			into(testresourcesOutput)
		}
	}
}

tasks.named<Delete>("cleanTest") {
	delete(testresourcesOutput)
}

tasks.withType<ValidatePlugins>().configureEach {
	failOnWarning.set(true)
	enableStricterValidation.set(true)
}
