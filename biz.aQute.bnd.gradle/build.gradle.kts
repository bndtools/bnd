plugins {
	groovy
	`kotlin-dsl`
}

interface Injected {
	@get:Inject val fs: FileSystemOperations
}

repositories {
	mavenCentral()
}

dependencies {
	compileOnly(localGroovy())
	compileOnly(gradleApi())
	testImplementation(gradleTestKit())
	testImplementation("org.spockframework:spock-core:2.0-groovy-3.0") {
		exclude(group = "org.codehaus.groovy")
		exclude(group = "org.junit.platform")
	}
}

if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_1_9)) {
	val jpmsOptions = parent?.extra?.get("jpmsOptions") as List<String>
	tasks.withType<GroovyCompile>().configureEach {
		groovyOptions.fork(mapOf("jvmArgs" to jpmsOptions))
	}
}

tasks.pluginUnderTestMetadata {
	getPluginClasspath().from(configurations.runtimeClasspath.allArtifacts.files, configurations.runtimeClasspath)
}

tasks.test {
	val testresources = layout.getProjectDirectory().dir("testresources")
	val target = layout.getBuildDirectory().dir("testresources")
	inputs.files(tasks.jar).withPropertyName("jar")
	inputs.dir(testresources).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("testresources")
	systemProperty("bnd_version", bnd.get("bnd_version"))
	systemProperty("org.gradle.warning.mode", gradle.getStartParameter().getWarningMode().name.toLowerCase())
	val injected = objects.newInstance<Injected>()
	doFirst {
		// copy test resources into build dir
		injected.fs.delete {
			delete(target)
		}
		injected.fs.copy {
			from(testresources)
			into(target)
		}
	}
}

tasks.release {
	dependsOn("groovydoc")
}

tasks.pluginDescriptors {
	enabled = false
}

tasks.validatePlugins {
	enabled = false
}

tasks.processResources {
	exclude("**/*.groovy", "**/*.kt")
}

// Groovy compile tasks: remove implicit dependency on java compile tasks
tasks.compileGroovy {
	classpath = sourceSets.main.get().compileClasspath
}
tasks.compileTestGroovy {
	classpath = sourceSets.test.get().compileClasspath
}

// Kotlin compile tasks: Add dependency on Groovy compile tasks
tasks.compileKotlin {
	classpath += files(sourceSets.main.get().groovy.classesDirectory)
}
tasks.compileTestKotlin {
	classpath += files(sourceSets.test.get().groovy.classesDirectory)
}

// Kotlin compile tasks: Use difference output folder since kotlin compile tasks seem to delete destination folder
afterEvaluate {
	tasks.compileKotlin {
		getDestinationDirectory().value(layout.getBuildDirectory().dir("classes/kotlin/main"))
	}
	tasks.compileTestKotlin {
		getDestinationDirectory().value(layout.getBuildDirectory().dir("classes/kotlin/test"))
	}
}
