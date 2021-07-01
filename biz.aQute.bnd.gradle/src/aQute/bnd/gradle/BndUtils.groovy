/**
 * BndUtils class.
 */

package aQute.bnd.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.logging.Logger
import org.gradle.util.GradleVersion

class BndUtils {
	private BndUtils() { }

	public static boolean isGradleCompatible(String requestedVersion) {
		return GradleVersion.current().compareTo(GradleVersion.version(requestedVersion)) >= 0
	}

	public static void logReport(var report, Logger logger) {
		if (logger.isWarnEnabled()) {
			report.getWarnings().forEach((String msg) -> {
				var location = report.getLocation(msg)
				if (location && location.file) {
					logger.warn("{}:{}: warning: {}", location.file, location.line, msg)
				} else {
					logger.warn("warning: {}", msg)
				}
			})
		}
		if (logger.isErrorEnabled()) {
			report.getErrors().forEach((String msg) -> {
				var location = report.getLocation(msg)
				if (location && location.file) {
					logger.error("{}:{}: error: {}", location.file, location.line, msg)
				} else {
					logger.error("error  : {}", msg)
				}
			})
		}
	}

	@CompileStatic
	public static ConfigurableFileCollection builtBy(ConfigurableFileCollection collection, Object... paths) {
		return collection.builtBy(paths.findAll { path ->
			path instanceof Task || path instanceof TaskProvider || path instanceof Buildable
		})
	}

	@CompileStatic
	public static Object unwrap(Object value) {
		return unwrap(value, false)
	}

	@CompileStatic
	public static Object unwrap(Object value, boolean optional) {
		if (value instanceof Provider) {
			value = optional ? value.getOrNull() : value.get()
		}
		if (value instanceof FileSystemLocation) {
			value = value.getAsFile()
		}
		return value
	}

	public static void jarLibraryElements(Task task, String configurationName) {
		Project project = task.getProject()
		var attributes = project.configurations[configurationName].attributes
		if (!Objects.equals(attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)?.getName(), LibraryElements.JAR)) {
			try {
				attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR))
				task.getLogger().info("Set {}:{} configuration attribute {} to {}", project.getPath(), configurationName, LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE))
			} catch (IllegalArgumentException e) {
				task.getLogger().info("Unable to set {}:{} configuration attribute {} to {}", project.getPath(), configurationName, LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, LibraryElements.JAR, e)
			}
		}
	}
}
