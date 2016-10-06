package aQute.bnd.maven.indexer.plugin;

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.indexer.impl.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "local-index", defaultPhase = PROCESS_RESOURCES)
public class LocalIndexerMojo extends AbstractMojo {
	private static final Logger	logger	= LoggerFactory.getLogger(LocalIndexerMojo.class);

	@Parameter(property = "bnd.indexer.input.dir", required=true)
	private File						inputDir;

	@Parameter(property = "bnd.indexer.output.file", defaultValue = "${project.build.directory}/index.xml")
	private File						outputFile;

	@Parameter(property = "bnd.indexer.base.file", required=false)
	private File						baseFile;

	@Parameter(property = "bnd.indexer.include.gzip", defaultValue = "true", readonly = true)
	private boolean						includeGzip;

	@Parameter(defaultValue = "false", readonly = true)
	private boolean						skip;

	private boolean						fail;

	public void execute() throws MojoExecutionException, MojoFailureException {

        if ( skip ) {
			logger.debug("skip project as configured");
			return;
		}

        if(baseFile == null) {
        	baseFile = outputFile.getParentFile();
        }
        
		logger.debug("Indexing dependencies in folder: {}", inputDir.getAbsolutePath());
		logger.debug("Outputting index to: {}", outputFile.getAbsolutePath());
		logger.debug("Producing additional gzip index: {}", includeGzip);
		logger.debug("URI paths will be relative to: {}", baseFile);

		Set<File> toIndex = new HashSet<>();
		toIndex.addAll(asList(inputDir.listFiles()));
		
		RepoIndex indexer = new RepoIndex();
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter("(name=*.jar)");
		} catch (InvalidSyntaxException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		indexer.addAnalyzer(new KnownBundleAnalyzer(), filter);

		indexer.addURLResolver(new BaseFileURLResolver());

		Map<String,String> config = new HashMap<String,String>();
		config.put(ResourceIndexer.PRETTY, "true");

		OutputStream output;
		try {
			outputFile.getParentFile().mkdirs();
			output = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		try {
			indexer.index(toIndex, output, config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		if (fail) {
			throw new MojoExecutionException("One or more URI lookups failed");
		}

		if (includeGzip) {
			File gzipOutputFile = new File(outputFile.getPath() + ".gz");

			try (InputStream is = new BufferedInputStream(new FileInputStream(outputFile));
					OutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipOutputFile))) {
				byte[] bytes = new byte[4096];
				int read;
				while ((read = is.read(bytes)) != -1) {
					gos.write(bytes, 0, read);
				}
			} catch (IOException ioe) {
				throw new MojoExecutionException("Unable to create the gzipped output file");
			}
		}

	}

	class BaseFileURLResolver implements URLResolver {
		public URI resolver(File file) throws Exception {
			try {
				logger.debug("Resolving {} relative to {}", file.getAbsolutePath(), baseFile.getAbsolutePath());
				Path relativePath = baseFile.getAbsoluteFile().toPath().relativize(file.getAbsoluteFile().toPath());
				logger.debug("Relative Path is: {}", relativePath);
				// Note that relativePath.toURI() gives the wrong answer for us!
				// We have to do some Windows related mashing here too :(
				URI relativeURI = URI.create(relativePath.toString().replace(File.separatorChar, '/'));
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
