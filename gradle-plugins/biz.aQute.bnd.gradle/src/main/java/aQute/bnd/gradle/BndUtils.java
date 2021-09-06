package aQute.bnd.gradle;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

import org.gradle.api.Buildable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
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

	public static <T> T unwrap(Provider<? extends T> provider) {
		return provider.get();
	}

	public static <T> T unwrapOrNull(Provider<? extends T> provider) {
		return provider.getOrNull();
	}

	public static File unwrapFile(FileSystemLocation location) {
		return location.getAsFile();
	}

	public static File unwrapFile(Provider<? extends FileSystemLocation> provider) {
		return unwrapFile(unwrap(provider));
	}

	public static File unwrapFileOrNull(Provider<? extends FileSystemLocation> provider) {
		return provider.isPresent() ? unwrapFile(provider) : null;
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

	@SuppressWarnings("deprecation")
	public static SourceSetContainer sourceSets(Project project) {
		SourceSetContainer sourceSets = isGradleCompatible("7.1") ? project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			: project.getConvention()
				.getPlugin(org.gradle.api.plugins.JavaPluginConvention.class)
				.getSourceSets();
		return sourceSets;
	}

	@SuppressWarnings("deprecation")
	public static DirectoryProperty distDirectory(Project project) {
		DirectoryProperty distDirectory = isGradleCompatible("7.1") ? project.getExtensions()
			.getByType(BasePluginExtension.class)
			.getDistsDirectory()
			: project.getConvention()
				.getPlugin(org.gradle.api.plugins.BasePluginConvention.class)
				.getDistsDirectory();
		return distDirectory;
	}

	@SuppressWarnings("deprecation")
	public static Provider<Directory> testResultsDir(Project project) {
		Provider<Directory> testResultsDir = isGradleCompatible("7.1") ? project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getTestResultsDir()
			: project.getLayout()
				.dir(project.provider(() -> project.getConvention()
					.getPlugin(org.gradle.api.plugins.JavaPluginConvention.class)
					.getTestResultsDir()));
		return testResultsDir;
	}

	public static <TOOL> Provider<TOOL> defaultToolFor(Project project,
		BiFunction<JavaToolchainService, JavaToolchainSpec, Provider<TOOL>> tool) {
		ExtensionContainer extensions = project.getExtensions();
		JavaToolchainSpec toolchain = extensions.getByType(JavaPluginExtension.class)
			.getToolchain();
		JavaToolchainService service = extensions.getByType(JavaToolchainService.class);
		return tool.apply(service, toolchain);
	}
}
