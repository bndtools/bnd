package aQute.bnd.maven.indexer.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.repository.SimpleIndexer;
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

	@Override
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
			.map(AntGlob::toPattern)
			.collect(Collectors.toList());
		List<Pattern> excludePatterns = excludes.stream()
			.map(AntGlob::toPattern)
			.collect(Collectors.toList());

		List<File> toIndex;
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
				.sorted()
				.distinct()
				.map(Path::toFile)
				.collect(Collectors.toList());
		} catch (IOException ioe) {
			throw new MojoExecutionException(ioe.getMessage(), ioe);
		}

		logger.debug("Included files: {}", toIndex);

		try {
			IO.mkdirs(outputFile.getParentFile());
			SimpleIndexer.index(toIndex, IO.outputStream(outputFile), baseFile.toURI(), false, indexName);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		if (includeGzip) {
			File gzipOutputFile = new File(outputFile.getAbsolutePath() + ".gz");
			try (OutputStream out = new GZIPOutputStream(IO.outputStream(gzipOutputFile))) {
				IO.copy(outputFile, out);
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to create the gzipped output file", e);
			}
		}
	}
}
