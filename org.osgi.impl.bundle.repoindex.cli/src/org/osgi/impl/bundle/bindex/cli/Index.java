/*
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.impl.bundle.bindex.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.util.tracker.ServiceTracker;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

@SuppressWarnings("restriction")
public class Index {

	/** the program name */
	static final String			PROGRAM_NAME					= "repoindex";

	public static final String	DEFAULT_FILENAME_UNCOMPRESSED	= "index.xml";
	public static final String	DEFAULT_FILENAME_COMPRESSED		= DEFAULT_FILENAME_UNCOMPRESSED + ".gz";

	/**
	 * Main entry point. See -help for options.
	 * 
	 * @param args Program arguments
	 */
	public static void main(String args[]) {
		try {
			// Configure PojoSR
			Map<String,Object> pojoSrConfig = new HashMap<>();
			pojoSrConfig.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, new ClasspathScanner());

			// Start PojoSR 'framework'
			Framework framework = new PojoServiceRegistryFactoryImpl().newFramework(pojoSrConfig);
			framework.init();
			framework.start();

			// Look for indexer and run index generation
			ServiceTracker<ResourceIndexer,ResourceIndexer> tracker = new ServiceTracker<>(
					framework.getBundleContext(), ResourceIndexer.class, null);
			tracker.open();
			ResourceIndexer index = tracker.waitForService(1000);
			if (index == null)
				throw new IllegalStateException("Timed out waiting for ResourceIndexer service.");

			// Process arguments
			Set<File> fileList = new LinkedHashSet<>();
			Map<String,String> config = new HashMap<>();
			File outputFile = processArgs(args, System.err, config, fileList, framework.getBundleContext());
			if (outputFile == null) {
				System.exit(1);
			}

			boolean verbose = Boolean.parseBoolean(config.get(ResourceIndexer.VERBOSE));
			if (verbose) {
				printCopyright(System.err);
			}

			try (@SuppressWarnings("null")
			OutputStream fos = Files.newOutputStream(outputFile.toPath())) {
				index.index(fileList, fos, config);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.exit(1);
		}

		// We really need to ensure all non-daemon threads -- which may have
		// been started by a bundle -- are terminated.
		System.exit(0);
	}

	private static File processArgs(String[] args, PrintStream err, Map<String,String> config,
			Collection< ? super File> fileList, BundleContext context) throws Exception {
		/*
		 * Parse the command line
		 */

		CommandLineOptions commandLineOptions = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(commandLineOptions);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			err.printf("Error during command-line parsing: %s%n", e.getLocalizedMessage());
			commandLineOptions.help = true;
		}

		/*
		 * Process command-line options
		 */

		/* print usage when so requested and exit */
		if (commandLineOptions.help) {
			try {
				/* can't be covered by a test */
				int cols = Integer.parseInt(System.getenv("COLUMNS")); //$NON-NLS-1$
				if (cols > 80) {
					parser.setUsageWidth(cols);
				}
			} catch (NumberFormatException e) {
				/* swallow, can't be covered by a test */
			}

			CommandLineOptions.usage(err, PROGRAM_NAME, parser);
			return null;
		}

		KnownBundleAnalyzer knownBundleAnalyzer = null;

		config.put(ResourceIndexer.REPOSITORY_NAME, commandLineOptions.repositoryName);

		if (commandLineOptions.stylesheetURL != null) {
			config.put(ResourceIndexer.STYLESHEET, commandLineOptions.stylesheetURL.toString());
		}

		File output = commandLineOptions.outputFile;

		if (commandLineOptions.verbose) {
			config.put(ResourceIndexer.VERBOSE, Boolean.TRUE.toString());
		}

		if (commandLineOptions.rootURL != null) {
			config.put(ResourceIndexer.ROOT_URL, commandLineOptions.rootURL.toString());
		}

		config.put(ResourceIndexer.URL_TEMPLATE, commandLineOptions.resourceUrlTemplate);

		if (commandLineOptions.licenseURL != null) {
			config.put(ResourceIndexer.LICENSE_URL, commandLineOptions.licenseURL.toString());
		}

		if (commandLineOptions.pretty) {
			config.put(ResourceIndexer.PRETTY, Boolean.TRUE.toString());
		}

		if (commandLineOptions.overrideBuiltinKnownBundles) {
			knownBundleAnalyzer = new KnownBundleAnalyzer(new Properties());
		}

		File knownBundlesExtraFile = commandLineOptions.knownBundlePropertiesFile;

		if (commandLineOptions.incrementOverride) {
			config.put(RepoIndex.REPOSITORY_INCREMENT_OVERRIDE, "");
		}

		if (commandLineOptions.fileList.isEmpty()) {
			fileList.clear();
		} else {
			for (File file : commandLineOptions.fileList) {
				fileList.add(new File(file.toURI().normalize().getPath()));
			}
		}

		if (knownBundleAnalyzer == null)
			knownBundleAnalyzer = new KnownBundleAnalyzer();

		if (knownBundlesExtraFile != null) {
			Properties props = loadPropertiesFile(knownBundlesExtraFile);
			knownBundleAnalyzer.setKnownBundlesExtra(props);
		}

		context.registerService(ResourceAnalyzer.class.getName(), knownBundleAnalyzer, null);

		return output;
	}

	private static Properties loadPropertiesFile(File knownBundles) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		try (InputStream stream = Files.newInputStream(knownBundles.toPath())) {
			props.load(stream);
		}
		return props;
	}

	public static void printCopyright(PrintStream out) throws IOException {
		String version = "";
		String copyright = "";
		Enumeration<URL> urls = Index.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			Manifest m = new Manifest(url.openStream());
			String bsn = m.getMainAttributes().getValue("Bundle-SymbolicName");
			if (bsn != null && bsn.equals("org.osgi.impl.bundle.repoindex.cli")) {
				version = m.getMainAttributes().getValue("Bundle-Version");
				copyright = m.getMainAttributes().getValue("Bundle-Copyright");
				break;
			}
		}

		out.println("RepoIndex | Resource Indexer v" + version);
		out.println(copyright);
	}
}
