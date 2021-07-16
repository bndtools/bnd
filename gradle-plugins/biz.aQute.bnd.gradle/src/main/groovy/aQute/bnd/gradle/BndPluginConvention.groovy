package aQute.bnd.gradle

import org.gradle.api.Project

/**
 * BndPluginConvention for Gradle.
 * 
 * @deprecated Replaced by BndPluginExtension.
 */
@Deprecated
class BndPluginConvention {
	private final BndPluginExtension extension

	BndPluginConvention(BndPluginExtension extension) {
		this.extension = extension
	}
	boolean bndis(String name) {
		return extension.is(name)
	}
	String bnd(String name) {
		return extension.get(name)
	}
	String bndMerge(String name) {
		return extension.merge(name)
	}
	Object bnd(String name, Object defaultValue) {
		return extension.get(name, defaultValue)
	}
	String bndProcess(String line) {
		return extension.process(line)
	}
	Object bndUnprocessed(String name, Object defaultValue) {
		return extension.unprocessed(name, defaultValue)
	}
}
