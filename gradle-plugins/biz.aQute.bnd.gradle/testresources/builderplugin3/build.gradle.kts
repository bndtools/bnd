/*
 *
 */

import aQute.bnd.gradle.Bundle

plugins {
	id("biz.aQute.bnd.builder")
}

val projectprop by extra("prop.project")
group = "test.bnd.gradle"
version = "1.0.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation("commons-codec:commons-codec:1.5")
	implementation("commons-lang:commons-lang:2.6")
	testImplementation("junit:junit:4.9")
}

tasks.jar {
	val taskprop by extra("prop.task")
	manifest {
		attributes(mapOf("Implementation-Title" to base.archivesName,
		"Implementation-Version" to "${'$'}{project.version}",
		"-includeresource" to "{${'$'}{.}/bar.txt}",
		"-include" to "${'$'}{.}/other.bnd",
		"Override" to "This should be overridden by the bnd file")
		)
	}
	bundle.setBnd(mapOf("Override" to "This will be ignored since there is a bnd file for this task"))
}

val bundleTask = tasks.register<Bundle>("bundle") {
	description = "Bundle"
	group = "build"
	from(sourceSets.test.map{ it.output })
	archiveClassifier.set("bundle")
	bundle {
		setBnd("""
-exportcontents: doubler.impl
-sources: true
My-Header: my-value
text: TEXT
Bundle-Name: ${'$'}{project.group}:${'$'}{task.archiveBaseName}-${'$'}{task.archiveClassifier}
Project-Name: ${'$'}{project.name}
Project-Dir: ${'$'}{project.dir}
Project-Output: ${'$'}{project.output}
Project-Sourcepath: ${'$'}{project.sourcepath}
Project-Buildpath: ${'$'}{project.buildpath}
""")
		bnd("Here: ${'$'}{.}")
		bnd(mapOf("-includeresource.lib" to "commons-lang-2.6.jar;lib:=true"))
		setSourceSet(sourceSets.test.get())
		setClasspath(configurations.compileClasspath)
		classpath(tasks.jar)
	}
	archiveVersion.set("1.1.0")
}

tasks.assemble {
	dependsOn(bundleTask)
}

artifacts {
	runtimeOnly(bundleTask)
}
