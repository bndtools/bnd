pluginManagement {
	plugins {
		id("com.gradle.plugin-publish") version("2.1.1")
		id("dev.hargrave.addmavendescriptor") version("1.1.0")
		id("com.gradle.develocity") version("4.4.3")
	}
}

plugins {
	id("com.gradle.develocity")
}

if (System.getenv("CI").toBoolean()) {
	develocity {
		buildScan {
			publishing.onlyIf { true }
			termsOfUseUrl.set("https://gradle.com/terms-of-service")
			termsOfUseAgree.set("yes")
		}
	}
}

rootProject.name = "gradle-plugins"
include("biz.aQute.bnd.gradle")
