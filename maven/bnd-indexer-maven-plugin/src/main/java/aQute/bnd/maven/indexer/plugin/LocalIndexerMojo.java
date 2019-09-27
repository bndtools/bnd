package aQute.bnd.maven.indexer.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.configuration.FileTree;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.lib.io.IO;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "local-index", defaultPhase = PROCESS_RESOURCES, threadSafe = true)
public class LocalIndexerMojo extends AbstractMojo {
	private static final Logger	logger		= LoggerFactory.getLogger(LocalIndexerMojo.class);

	@Parameter(property = "bnd.indexer.input.dir", required = true)
	private File				inputDir;

	@Parameter(property = "bnd.indexer.output.file", defaultValue = "${project.build.directory}/index.xml")
	private File				outputFile;

	@Parameter(property = "bnd.indexer.base.file")
	private File				baseFile;

	@Parameter
	private FileTree			indexFiles	= new FileTree();

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true")
	private boolean				includeGzip;

	@Parameter(property = "bnd.indexer.skip", defaultValue = "false")
	private boolean				skip;

	@Parameter(property = "bnd.indexer.absolute", defaultValue = "false")
	private boolean				absolute;

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

		logger.debug("Indexing dependencies in folder: {}", inputDir.getAbsolutePath());
		logger.debug("Outputting index to: {}", outputFile.getAbsolutePath());
		logger.debug("Producing additional gzip index: {}", includeGzip);
		if (absolute) {
			logger.debug("URI paths will be absolute");
		} else {
			logger.debug("URI paths will be relative to: {}", baseFile);
		}

		try {
			List<File> toIndex = indexFiles.getFiles(inputDir, "**/*.jar");
			if (absolute) {
				toIndex = toIndex.stream()
					.map(f -> f.toPath()
						.normalize()
						.toFile())
					.collect(Collectors.toList());
			}
			logger.debug("Included files: {}", toIndex);
			IO.mkdirs(outputFile.getParentFile());
			final SimpleIndexer simpleIndexer = new SimpleIndexer().files(toIndex);
			if (!absolute) {
				simpleIndexer.base(baseFile.toURI());
			}
			simpleIndexer.name(indexName)
				.index(outputFile);
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
