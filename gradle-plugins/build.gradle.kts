if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_1_9)) {
	val jpmsOptions by extra(listOf(
		"--add-opens=java.base/java.lang=ALL-UNNAMED",
		"--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
		"--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
		"--add-opens=java.base/java.io=ALL-UNNAMED",
		"--add-opens=java.base/java.net=ALL-UNNAMED",
		"--add-opens=java.base/java.nio=ALL-UNNAMED",
		"--add-opens=java.base/java.util=ALL-UNNAMED",
		"--add-opens=java.base/java.util.jar=ALL-UNNAMED",
		"--add-opens=java.base/java.util.regex=ALL-UNNAMED",
		"--add-opens=java.base/java.util.zip=ALL-UNNAMED",
		"--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.file=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.ftp=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.http=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED",
		"--add-opens=java.base/sun.net.www.protocol.jrt=ALL-UNNAMED"
	))
}

tasks.register("clean") {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

tasks.register("testClasses") {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

tasks.register("build") {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}

tasks.register("publish") {
	val taskName = name
	dependsOn(subprojects.map { it.tasks.named(taskName) })
}
