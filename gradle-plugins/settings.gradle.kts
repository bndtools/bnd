pluginManagement {
	plugins {
		id("com.gradle.plugin-publish") version("2.1.1")
		id("dev.hargrave.addmavendescriptor") version("1.1.0")
	}
}

rootProject.name = "gradle-plugins"
include("biz.aQute.bnd.gradle")
