pluginManagement {
	plugins {
		id("com.gradle.plugin-publish") version("1.0.0")
		id("dev.hargrave.addmavendescriptor") version("1.0.0")
		id("com.gradle.enterprise") version("3.11.1")
	}
}

plugins {
	id("com.gradle.enterprise")
}

if (System.getenv("CI").toBoolean()) {
	gradleEnterprise {
		buildScan {
			publishAlways()
			termsOfServiceUrl = "https://gradle.com/terms-of-service"
			termsOfServiceAgree = "yes"
		}
	}
}

rootProject.name = "gradle-plugins"
include("biz.aQute.bnd.gradle")
