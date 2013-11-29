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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

public class Index {

	/** the program name */
	static final String PROGRAM_NAME = "repoindex";

	public static final String DEFAULT_FILENAME_UNCOMPRESSED = "index.xml";
	public static final String DEFAULT_FILENAME_COMPRESSED = DEFAULT_FILENAME_UNCOMPRESSED + ".gz";

	/**
	 * Main entry point. See -help for options.
	 * 
	 * @param args
	 *            Program arguments
	 */
	public static void main(String args[]) {
		try {

			// Configure PojoSR
			Map<String, Object> pojoSrConfig = new HashMap<String, Object>();
			pojoSrConfig.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, new ClasspathScanner());

			// Start PojoSR 'framework'
			Framework framework = new PojoServiceRegistryFactoryImpl().newFramework(pojoSrConfig);
			framework.init();
			framework.start();

			// Look for indexer and run index generation
			ServiceTracker tracker = new ServiceTracker(framework.getBundleContext(), ResourceIndexer.class.getName(), null);
			tracker.open();
			ResourceIndexer index = (ResourceIndexer) tracker.waitForService(1000);
			if (index == null)
				throw new IllegalStateException("Timed out waiting for ResourceIndexer service.");

			// Process arguments
			Set<File> fileList = new LinkedHashSet<File>();
			Map<String, String> config = new HashMap<String, String>();
			File outputFile = processArgs(args, System.err, config, fileList, framework.getBundleContext());
			if (outputFile == null) {
				System.exit(1);
			}

			boolean verbose = Boolean.parseBoolean(config.get(ResourceIndexer.VERBOSE));
			if (verbose) {
				printCopyright(System.err);
			}

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(outputFile);
				index.index(fileList, fos, config);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						/* swallow */
					}
					fos = null;
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		// We really need to ensure all non-daemon threads -- which may have
		// been started by a bundle -- are terminated.
		System.exit(0);
	}

	private static File processArgs(String[] args, PrintStream err, Map<String, String> config, Collection<? super File> fileList, BundleContext context) throws Exception {
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
			return null;
		}
		fileList.addAll(commandLineOptions.fileList);

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
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(knownBundles);
			props.load(stream);
		} finally {
			if (stream != null)
				stream.close();
		}
		return props;
	}

	public static void printCopyright(PrintStream out) {
		out.println("Bindex2 | Resource Indexer v1.0");
		out.println("(c) 2012 OSGi, All Rights Reserved");
	}
}
