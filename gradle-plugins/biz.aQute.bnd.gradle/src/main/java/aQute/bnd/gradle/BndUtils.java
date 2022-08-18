package aQute.bnd.gradle;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import aQute.service.reporter.Report;
import aQute.service.reporter.Report.Location;
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

/**
 * BndUtils class.
 */
public class BndUtils {
	private BndUtils() {}

	/**
	 * Return whether the current Gradle is at least the specified version.
	 *
	 * @param requestedVersion The requested Gradle version.
	 * @return Whether the current Gradle is at least the specified version.
	 */
	public static boolean isGradleCompatible(String requestedVersion) {
		return GradleVersion.current()
			.compareTo(GradleVersion.version(requestedVersion)) >= 0;
	}

	/**
	 * Log the Report information.
	 *
	 * @param report The Report object to check.
	 * @param logger The Logger.
	 */
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

	/**
	 * Set the builtBy on the collection for the specified paths.
	 *
	 * @param collection The ConfigurableFileCollection.
	 * @param paths The paths to use for builtBy.
	 * @return The specified ConfigurableFileCollection.
	 */
	public static ConfigurableFileCollection builtBy(ConfigurableFileCollection collection, Object... paths) {
		Object[] builtBy = Arrays.stream(paths)
			.filter(path -> path instanceof Task || path instanceof TaskProvider || path instanceof Buildable)
			.toArray();
		return collection.builtBy(builtBy);
	}

	/**
	 * Return the value of the specified Provider.
	 *
	 * @param <T> The type of the Provider's value.
	 * @param provider The Provider.
	 * @return The value of the specified Provider.
	 * @throws IllegalStateException If the Provider does not have a value.
	 */
	public static <T> T unwrap(Provider<? extends T> provider) {
		return provider.get();
	}

	/**
	 * Return the value of the specified Provider or {@code null} if the
	 * Provider does not have a value.
	 *
	 * @param <T> The type of the Provider's value.
	 * @param provider The Provider.
	 * @return The value of the specified Provider or {@code null} if the
	 *         Provider does not have a value.
	 */
	public static <T> Optional<T> unwrapOptional(Provider<? extends T> provider) {
		return Optional.ofNullable(provider.getOrNull());
	}

	/**
	 * Return the File object of the specified FileSystemLocation.
	 *
	 * @param location The FileSystemLocation.
	 * @return The File object of the specified FileSystemLocation.
	 */
	public static File unwrapFile(FileSystemLocation location) {
		return location.getAsFile();
	}

	/**
	 * Return the File object of the specified Provider.
	 *
	 * @param provider The Provider.
	 * @return The File object of the specified Provider.
	 * @throws IllegalStateException If the Provider does not have a value.
	 */
	public static File unwrapFile(Provider<? extends FileSystemLocation> provider) {
		return unwrapFile(unwrap(provider));
	}

	/**
	 * Return the File object of the specified Provider or {@code null} if the
	 * Provider does not have a value.
	 *
	 * @param provider The Provider.
	 * @return The File object of the specified Provider or {@code null} if the
	 *         Provider does not have a value.
	 */
	public static Optional<File> unwrapFileOptional(Provider<? extends FileSystemLocation> provider) {
		return provider.isPresent() ? Optional.of(unwrapFile(provider)) : Optional.empty();
	}

	/**
	 * Set the Library Element Attribute of the specified configuration's
	 * attributes to Jar.
	 *
	 * @param task The Task.
	 * @param configurationName The configuration name.
	 */
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

	/**
	 * Return the SourceSetContainer for the specified Project.
	 *
	 * @param project The Project.
	 * @return The SourceSetContainer for the specified Project.
	 */
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

	/**
	 * Return the distDirectory property for the specified Project.
	 *
	 * @param project The Project.
	 * @return The distDirectory property for the specified Project.
	 */
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

	/**
	 * Return the testResultsDir property for the specified Project.
	 *
	 * @param project The Project.
	 * @return The testResultsDir property for the specified Project.
	 */
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

	/**
	 * Return a tool Provider for the specified Project.
	 *
	 * @param <TOOL> The tool type.
	 * @param project The Project.
	 * @param tool The function which returns the tool Provider.
	 * @return A tool Provider for the specified Project.
	 */
	public static <TOOL> Provider<TOOL> defaultToolFor(Project project,
		BiFunction<JavaToolchainService, JavaToolchainSpec, Provider<TOOL>> tool) {
		ExtensionContainer extensions = project.getExtensions();
		JavaToolchainSpec toolchain = extensions.getByType(JavaPluginExtension.class)
			.getToolchain();
		JavaToolchainService service = extensions.getByType(JavaToolchainService.class);
		return tool.apply(service, toolchain);
	}
}
