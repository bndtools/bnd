package aQute.bnd.gradle;

import java.util.Arrays;
import java.util.Objects;

import org.gradle.api.Buildable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;

import aQute.service.reporter.Report;
import aQute.service.reporter.Report.Location;

/**
 * BndUtils class.
 */
public class BndUtils {
	private BndUtils() {}

	public static boolean isGradleCompatible(String requestedVersion) {
		return GradleVersion.current()
			.compareTo(GradleVersion.version(requestedVersion)) >= 0;
	}

	public static void logReport(Report report, Logger logger) {
		if (logger.isWarnEnabled()) {
			report.getWarnings()
				.forEach((String msg) -> {
					Location location = report.getLocation(msg);
					if ((location != null) && (location.file != null)) {
						logger.warn("{}:{}: warning: {}", location.file, location.line, msg);
					} else {
						logger.warn("warning: {}", msg);
					}
				});
		}
		if (logger.isErrorEnabled()) {
			report.getErrors()
				.forEach((String msg) -> {
					Location location = report.getLocation(msg);
					if ((location != null) && (location.file != null)) {
						logger.error("{}:{}: error: {}", location.file, location.line, msg);
					} else {
						logger.error("error  : {}", msg);
					}
				});
		}
	}

	public static ConfigurableFileCollection builtBy(ConfigurableFileCollection collection, Object... paths) {
		Object[] builtBy = Arrays.stream(paths)
			.filter(path -> path instanceof Task || path instanceof TaskProvider || path instanceof Buildable)
			.toArray();
		return collection.builtBy(builtBy);
	}

	public static <T> T unwrap(Object value) {
		return unwrap(value, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T unwrap(Object value, boolean optional) {
		if (value instanceof Provider) {
			value = optional ? ((Provider<?>) value).getOrNull() : ((Provider<?>) value).get();
		}
		if (value instanceof FileSystemLocation) {
			value = ((FileSystemLocation) value).getAsFile();
		}
		return (T) value;
	}

	public static void jarLibraryElements(Task task, String configurationName) {
		Project project = task.getProject();
		AttributeContainer attributes = project.getConfigurations()
			.getByName(configurationName)
			.getAttributes();
		LibraryElements attribute = attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE);
		if ((attribute == null) || !Objects.equals(attribute.getName(), LibraryElements.JAR)) {
			try {
				attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects()
					.named(LibraryElements.class, LibraryElements.JAR));
				task.getLogger()
					.info("Set {}:{} configuration attribute {} to {}", project.getPath(), configurationName,
						LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
						attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE));
			} catch (IllegalArgumentException e) {
				task.getLogger()
					.info("Unable to set {}:{} configuration attribute {} to {}", project.getPath(), configurationName,
						LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, LibraryElements.JAR, e);
			}
		}
	}
}
