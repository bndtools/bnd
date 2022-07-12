val localrepo: String? = System.getProperty("maven.repo.local")
localrepo?.let {
	var rootGradle: Gradle = gradle
	while (rootGradle.parent != null) {
		rootGradle = rootGradle.parent!!
	}
	val maven_repo_local by extra(rootGradle.startParameter.currentDir.resolve(it).normalize().absolutePath)
}

val clean by tasks.registering {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

val testClasses by tasks.registering {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

val build by tasks.registering {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

val publish by tasks.registering {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}
