pluginManagement {
	plugins {
		id("com.gradle.plugin-publish") version "1.0.0"
		id("dev.hargrave.addmavendescriptor") version "1.0.0"
	}
}

rootProject.name = "gradle-plugins"
include("biz.aQute.bnd.gradle")
