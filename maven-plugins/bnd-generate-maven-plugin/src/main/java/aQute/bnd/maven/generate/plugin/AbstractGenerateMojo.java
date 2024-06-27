package aQute.bnd.maven.generate.plugin;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

import aQute.bnd.build.Project;
import aQute.bnd.result.Result;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGenerateMojo extends AbstractMojo {

	protected final Logger			logger	= LoggerFactory.getLogger(getClass());

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject										project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings											settings;

	@Parameter(defaultValue = "${mojoExecution}", readonly = true)
	MojoExecution												mojoExecution;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession								repositorySession;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File												targetDir;

	/**
	 * Skip this goal.
	 */
	@Parameter(property = "bnd.generate.skip", defaultValue = "false")
	boolean														skip;

	/**
	 * The Mojo by itself does nothing. Dependent on its configuration, it will
	 * try to find a suitable generator in one of the dependencies in this list.
	 */
	@Parameter(property = "externalPlugins", required = false)
	List<Dependency>				externalPlugins;

	/**
	 * Allows multiple steps
	 */
	@Parameter(property = "steps", required = false)
	List<Step>						steps;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession										session;

	@Component
	private RepositorySystem									system;

	/**
	 * File path to a bnd file containing bnd instructions for this project.
	 * Defaults to {@code bnd.bnd}. The file path can be an absolute or relative
	 * to the project directory.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd
	 * instructions, if any, for the parent project.
	 */
	@Parameter(defaultValue = Project.BNDFILE)
	// This is not used and is for doc only; see {@link
	// BndConfiguration#loadProperties(Processor)}
	@SuppressWarnings("unused")
	String							bndfile;

	/**
	 * Bnd instructions for this project specified directly in the pom file.
	 * This is generally be done using a {@code <![CDATA[]]>} section. If the
	 * project has a {@link #bndfile}, then this configuration element
	 * is ignored.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd
	 * instructions, if any, for the parent project.
	 */
	@Parameter
	// This is not used and is for doc only; See {@link
	// BndConfiguration#loadProperties(Processor)}
	@SuppressWarnings("unused")
	String							bnd;

	public void doExecute(boolean includeTestDependencies) throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}

		int errors;

		List<Dependency> normalizedDependencies = new ArrayList<>();
		if (externalPlugins != null) {
			for (Dependency dependency : externalPlugins) {
				normalizedDependencies.add(normalizeDependency(dependency));
			}
		}
		Properties additionalProperties = new Properties();
		String instruction = steps.stream()
			.map(this::mapStep)
			.collect(joining(","));
		if (!instruction.isEmpty()) {
			logger.info("created instructions from steps: {}", instruction);
			additionalProperties.put("-generate.maven", instruction);
		}
		try {
			BndContainer container = new BndContainer.Builder(project, session, repositorySession, system)
				.setDependencies(normalizedDependencies)
				.setAdditionalProperties(additionalProperties)
					.build();

			GenerateOperation operation = getOperation();

			errors = container.generate("generating", targetDir, operation, settings, mojoExecution,
				includeTestDependencies);

		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (errors > 0)
			throw new MojoFailureException(errors + " errors found");
	}

	private GenerateOperation getOperation() {
		return (taskName, project) -> {
			int errors = 0;
			try {
				List<File> files = getProjectFiles();
				if (project.getGenerate()
					.needsBuild(files)) {
					Result<Set<File>> result = project.getGenerate()
						.generate(false);
					if (result.isErr()) {
						result.error()
						.ifPresent(error -> logger.error("Error   : {}", error));
						errors++;
					} else {
						Set<File> set = result.unwrap();
						logger.info("Files generated: " + set.size());
						set.forEach(f -> logger.info("  " + f.getPath()));
					}
				} else {
					logger.info("Generated Code seems up to date, no run required.");
				}
			} finally {
				errors += BndContainer.report(project);
			}
			return errors;
		};
	}

	private List<File> getProjectFiles() {
		List<File> files = new ArrayList<>();
		addProject(project, files);
		return files;
	}

	private void addProject(MavenProject currentProject, List<File> files) {
		if (currentProject == null) {
			return;
		}
		if (currentProject.getFile() != null) {
			files.add(currentProject.getFile());
		}
		addProject(currentProject.getParent(), files);
	}

	private String mapStep(Step step) {
		StringJoiner joiner = new StringJoiner(";");
		joiner.add(step.getTrigger());
		joiner.add("output=" + step.getOutput());
		joiner.add("clear=" + step.isClear());
		if (step.getGenerateCommand() != null) {
			joiner.add("generate=\"" + step.getGenerateCommand() + "\"");
		}
		if (step.getSystemCommand() != null) {
			joiner.add("system=\"" + step.getSystemCommand() + "\"");
		}
		step.getProperties()
			.forEach((k, v) -> joiner.add(k + "=\"" + v + "\""));
		return joiner.toString();
	}

	private Dependency normalizeDependency(Dependency dependency) throws MojoExecutionException {
		if(dependency.getVersion() != null) {
			return dependency;
		} else {
			List<Dependency> deps = project.getDependencyManagement() != null ? project.getDependencyManagement()
				.getDependencies() : Collections.emptyList();
			return deps
				.stream()
				.filter(d -> d.getArtifactId()
					.equals(dependency.getArtifactId())
					&& d.getGroupId()
						.equals(dependency.getGroupId()))
				.findFirst()
				.map(Dependency::clone)
				.orElseThrow(() -> new MojoExecutionException(dependency, "Version is missing",
					"The Version of the " + dependency + " is missing"));
		}
	}
}
