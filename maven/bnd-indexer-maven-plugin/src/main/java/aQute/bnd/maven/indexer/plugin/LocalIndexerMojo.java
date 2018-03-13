package aQute.bnd.maven.indexer.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;
import aQute.libg.glob.AntGlob;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "local-index", defaultPhase = PROCESS_RESOURCES)
public class LocalIndexerMojo extends AbstractMojo {
	private static final Logger	logger		= LoggerFactory.getLogger(LocalIndexerMojo.class);

	@Parameter(property = "bnd.indexer.input.dir", required = true)
	private File				inputDir;

	@Parameter(property = "bnd.indexer.output.file", defaultValue = "${project.build.directory}/index.xml")
	private File				outputFile;

	@Parameter(property = "bnd.indexer.base.file")
	private File				baseFile;

	@Parameter(property = "bnd.indexer.input.dir.includes")
	private Set<String>			includes	= new HashSet<String>();

	@Parameter(property = "bnd.indexer.input.dir.excludes")
	private Set<String>			excludes	= new HashSet<String>();

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true")
	private boolean				includeGzip;

	@Parameter(property = "bnd.indexer.skip", defaultValue = "false")
	private boolean				skip;

	/**
	 * This configuration parameter is used to set the name of the repository in
	 * the generated index
	 */
	@Parameter(property = "bnd.indexer.name", defaultValue = "${project.artifactId}")
	private String				indexName;

	private boolean				fail;

	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		if (baseFile == null) {
			baseFile = outputFile.getParentFile();
		}

		if (!inputDir.isDirectory()) {
			throw new MojoExecutionException("inputDir does not refer to a directory");
		}

		if (includes.isEmpty()) {
			includes.add("**/*.jar");
		}

		logger.debug("Indexing dependencies in folder: {}", inputDir.getAbsolutePath());
		logger.debug("Including files: {}", includes);
		logger.debug("Excluding files: {}", excludes);
		logger.debug("Outputting index to: {}", outputFile.getAbsolutePath());
		logger.debug("Producing additional gzip index: {}", includeGzip);
		logger.debug("URI paths will be relative to: {}", baseFile);

		List<Pattern> includePatterns = includes.stream()
			.map(s -> AntGlob.toPattern(s))
			.collect(Collectors.toList());
		List<Pattern> excludePatterns = excludes.stream()
			.map(s -> AntGlob.toPattern(s))
			.collect(Collectors.toList());

		Set<File> toIndex;

		try {
			toIndex = Files.find(inputDir.toPath(), Integer.MAX_VALUE, (p, a) -> {
				String path = p.toString();

				return includePatterns.stream()
					.anyMatch(i -> i.matcher(path)
						.matches())
					&& !excludePatterns.stream()
						.anyMatch(e -> e.matcher(path)
							.matches());
			})
				.map(p -> p.toFile())
				.collect(Collectors.toSet());
		} catch (IOException ioe) {
			throw new MojoExecutionException(ioe.getMessage(), ioe);
		}

		logger.debug("Included files: {}", toIndex);

		BaseFileURLResolver baseFileURLResolver = new BaseFileURLResolver();
		ResourcesRepository resourcesRepository = new ResourcesRepository();
		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();

		for (File file : toIndex) {
			ResourceBuilder resourceBuilder = new ResourceBuilder();
			try {
				resourceBuilder.addFile(file, baseFileURLResolver.resolver(file));
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
			resourcesRepository.add(resourceBuilder.build());
		}

		try {
			IO.mkdirs(outputFile.getParentFile());
			xmlResourceGenerator.name(indexName)
				.repository(resourcesRepository)
				.save(outputFile);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (fail) {
			throw new MojoExecutionException("One or more URI lookups failed");
		}

		if (includeGzip) {
			File gzipOutputFile = new File(outputFile.getPath() + ".gz");
			try {
				xmlResourceGenerator.save(gzipOutputFile);
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to create the gzipped output file");
			}
		}

	}

	class BaseFileURLResolver {
		public URI resolver(File file) throws Exception {
			try {
				logger.debug("Resolving {} relative to {}", file.getAbsolutePath(), baseFile.getAbsolutePath());
				Path relativePath = baseFile.getAbsoluteFile()
					.toPath()
					.relativize(file.getAbsoluteFile()
						.toPath());
				logger.debug("Relative Path is: {}", relativePath);
				// Note that relativePath.toURI() gives the wrong answer for us!
				// We have to do some Windows related mashing here too :(
				URI relativeURI = URI.create(relativePath.toString()
					.replace(File.separatorChar, '/'));
				logger.debug("Relative URI is: {}", relativeURI);
				return relativeURI;
			} catch (Exception e) {
				logger.error("Exception resolving URI", e);
				fail = true;
				throw e;
			}
		}
	}
}
