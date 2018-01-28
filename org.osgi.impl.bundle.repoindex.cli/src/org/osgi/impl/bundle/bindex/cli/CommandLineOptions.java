package org.osgi.impl.bundle.bindex.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.osgi.service.indexer.ResourceIndexer;

public class CommandLineOptions {

	public CommandLineOptions() {
		super();
		try {
			rootURL = new File(System.getProperty("user.dir")).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/*
	 * Pretty
	 */

	public static final boolean	DEFAULT_PRETTY	= false;

	boolean						pretty			= DEFAULT_PRETTY;

	@Option(name = "--pretty", handler = BooleanOptionHandler.class, usage = "Non-compressed, indented output. Forces the output filename to "
			+ Index.DEFAULT_FILENAME_UNCOMPRESSED)
	public void setPretty(boolean pretty) {
		this.pretty = pretty;
		if (!pretty) {
			outputFile = new File(Index.DEFAULT_FILENAME_COMPRESSED);
		} else {
			outputFile = new File(Index.DEFAULT_FILENAME_UNCOMPRESSED);
		}
	}

	/*
	 * Output File
	 */

	public static final String	DEFAULT_OUTPUT_FILENAME					= DEFAULT_PRETTY
			? Index.DEFAULT_FILENAME_UNCOMPRESSED : Index.DEFAULT_FILENAME_COMPRESSED;

	@Option(name = "-r", metaVar = "/output/file/index.xml(.gz)", usage = "Output file name " + "(default = "
			+ Index.DEFAULT_FILENAME_COMPRESSED + " for normal output, " + Index.DEFAULT_FILENAME_UNCOMPRESSED
			+ " for pretty output)")
	File						outputFile								= new File(DEFAULT_OUTPUT_FILENAME);

	/*
	 * Repository Name
	 */

	public static final String	DEFAULT_REPOSITORY_NAME					= ResourceIndexer.REPOSITORYNAME_DEFAULT;

	@Option(name = "-n", metaVar = "RepositoryName", usage = "Repository name (default = " + DEFAULT_REPOSITORY_NAME
			+ ")")
	String						repositoryName							= DEFAULT_REPOSITORY_NAME;

	/*
	 * Known Bundles Properties File
	 */

	@Option(name = "-k", metaVar = "/known/bundles.properties", usage = "Load extra known-bundles data from file (default = none)")
	File						knownBundlePropertiesFile				= null;

	/*
	 * Override Built-In Known Bundles
	 */

	public static final boolean	DEFAULT_OVERRIDE_BUILTIN_KNOWN_BUNDLES	= false;

	@Option(name = "-K", handler = BooleanOptionHandler.class, usage = "Override built-in known-bundles data")
	boolean						overrideBuiltinKnownBundles				= DEFAULT_OVERRIDE_BUILTIN_KNOWN_BUNDLES;

	/*
	 * Resource URL Template
	 */

	public static final String	DEFAULT_RESOURCE_URL_TEMPLATE			= "%p%f";

	@Option(name = "-t", metaVar = DEFAULT_RESOURCE_URL_TEMPLATE, usage = "Resource URL template. Use %s for symbolic name, %v for version, %f for filename and %p for dirpath (default = "
			+ DEFAULT_RESOURCE_URL_TEMPLATE + ")")
	String						resourceUrlTemplate						= DEFAULT_RESOURCE_URL_TEMPLATE;

	/*
	 * Root Directory
	 */

	URL							rootURL									= null;

	@Option(name = "-d", metaVar = "/root/dir", usage = "Root directory " + "(default = the current directory)")
	public void setRootURL(File rootURL) throws MalformedURLException, IOException {
		if (!rootURL.isDirectory()) {
			throw new IOException(rootURL + " is not a directory");
		}

		this.rootURL = rootURL.toURI().normalize().toURL();

		/* make sure the URL ends with a slash */
		String rootURLString = this.rootURL.toString();
		if (!rootURLString.endsWith("/")) {
			this.rootURL = new URL(rootURLString + "/");
		}
	}

	/*
	 * License URL
	 */

	@Option(name = "-l", metaVar = "file:license.html", usage = "License file " + "(default = none)")
	URL							licenseURL			= null;

	/*
	 * Verbose
	 */

	public static final boolean	DEFAULT_VERBOSE		= false;

	@Option(name = "-v", handler = BooleanOptionHandler.class, usage = "Verbose reporting")
	boolean						verbose				= DEFAULT_VERBOSE;

	/*
	 * Stylesheet URL
	 */

	@Option(name = "-stylesheet", metaVar = "http://some/url.xsl", usage = "Stylesheet URL, for example: http://bnd.bndtools.org/static/obr2html.xsl (default = none)")
	URL							stylesheetURL		= null;

	/*
	 * Increment Override
	 */

	public static final boolean	DEFAULT_NOINCREMENT	= false;

	@Option(name = "--noincrement", handler = BooleanOptionHandler.class, usage = "Increment override")
	boolean						incrementOverride	= DEFAULT_NOINCREMENT;

	/*
	 * File List
	 */

	@Argument(metaVar = "<file> [<file>*]", required = false, index = 0, usage = "The directories and/or files to index. Must be specified at least once, can be specified multiple times (default = none)")
	List<File>					fileList			= new LinkedList<>();

	/*
	 * Help
	 */

	@Option(name = "-h", handler = BooleanOptionHandler.class, usage = "Help")
	boolean						help				= false;

	/*
	 * Utilities
	 */

	static void usage(PrintStream out, String programName, CmdLineParser parser) {
		out.println();
		out.printf("%s [options...] <file> [<file>*]%n", programName);
		parser.printUsage(out);
		out.println();
		out.printf(
				"Example: java -jar org.osgi.impl.bundle.repoindex.cli.jar /some/path/with/bundels /some/bundle/file.jar%n");
	}
}