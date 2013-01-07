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

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.util.tracker.ServiceTracker;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

public class Index {
	
	public static final String DEFAULT_FILENAME_UNCOMPRESSED = "index.xml";
	public static final String DEFAULT_FILENAME_COMPRESSED = "index.xml" + ".gz";
	
	/**
	 * Main entry point. See -help for options.
	 * 
	 * @param args
	 *            Program arguments
	 */
	public static void main(String args[]) {
		try {
			printCopyright(System.err);
			
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
			File outputFile = processArgs(args, config, fileList, framework.getBundleContext());
			
			// Run
			if (fileList.isEmpty())
				printUsage();
			else try {
				index.index(fileList, new FileOutputStream(outputFile), config);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		// We really need to ensure all non-daemon threads -- which may have been started by a bundle -- are terminated.
		System.exit(0);
	}
	
	private static File processArgs(String[] args, Map<String, String> config, Collection<? super File> fileList, BundleContext context) throws Exception {
		File output = new File(DEFAULT_FILENAME_COMPRESSED);
		
		KnownBundleAnalyzer knownBundleAnalyzer = null;
		File knownBundlesExtraFile = null;
		
		for (int i = 0; i < args.length; i++) {
			try {
				if (args[i].startsWith("-n")) {
					String repoName = args[++i];
					config.put(ResourceIndexer.REPOSITORY_NAME, repoName);
				} else if (args[i].equals("-stylesheet")) {
					String styleSheet = args[++i];
					config.put(ResourceIndexer.STYLESHEET, styleSheet);
				} else if (args[i].startsWith("-r")) {
					output = new File(args[++i]);
				} else if (args[i].startsWith("-v")) {
					config.put(ResourceIndexer.VERBOSE, Boolean.TRUE.toString());
				} else if (args[i].startsWith("-d")) {
					config.put(ResourceIndexer.ROOT_URL, args[++i]);
				} else if (args[i].startsWith("-t")) {
					String urlTemplate = args[++i];
					config.put(ResourceIndexer.URL_TEMPLATE, urlTemplate);
				} else if (args[i].startsWith("-l")) {
					String licenceUrl = args[++i];
					config.put(ResourceIndexer.LICENSE_URL, licenceUrl);
				} else if (args[i].equalsIgnoreCase("--pretty")) {
					output = new File(DEFAULT_FILENAME_UNCOMPRESSED);
					config.put(ResourceIndexer.PRETTY, Boolean.toString(true));
				} else if (args[i].equals("-K")) {
					knownBundleAnalyzer = new KnownBundleAnalyzer(new Properties());
				} else if (args[i].equals("-k")) {
					knownBundlesExtraFile = new File(args[++i]);
				} else if(args[i].equalsIgnoreCase("--noincrement")) {
					config.put("-repository.increment.override", "");
				} else if (args[i].startsWith("-h")) {
					printUsage();
				} else if (args[i].startsWith("-")) {
					throw new Exception("Unknown argument");
				} else {
					fileList.add(new File(args[i]));
				}
			} catch (Exception e) {
				System.err.println("Error in " + args[i] + " : " + e.getMessage());
				System.exit(1);
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

	private static Properties loadPropertiesFile(File knownBundles)
			throws FileNotFoundException, IOException {
		Properties props = new Properties();
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(knownBundles);
			props.load(stream);
		} finally {
			if (stream != null) stream.close();
		}
		return props;
	}
	
	public static void printCopyright(PrintStream out) {
		out.println("Bindex2 | Resource Indexer v1.0");
		out.println("(c) 2012 OSGi, All Rights Reserved");
	}

	private static void printUsage() {
		System.err
				.printf("Arguments:%n" //
						+ "  [-r index.xml(.gz)]                                              --> Output file name.%n" //
						+ "  [--pretty]                                                       --> Non-compressed, indented output.%n" //
						+ "  [-n Untitled]                                                    --> Repository name.%n"
						+ "  [-k known-bundles.properties]                                    --> Load extra known-bundles data from file.%n"
						+ "  [-K]                                                             --> Override built-in known-bundles data.%n"
						+ "  [-t \"%%s\" symbolic name \"%%v\" version \"%%f\" filename \"%%p\" dirpath ] --> Resource URL template.%n" //
						+ "  [-d rootdir]                                                     --> Root directory.%n"
						+ "  [-l file:license.html]                                           --> Licence file.%n"
						+ "  [-v]                                                             --> Verbose reporting.%n"
						+ "  [-stylesheet http://www.osgi.org/www/obr2html.xsl]               --> Stylesheet URL.%n"
						+ "  <file> [<file>*]%n");
	}
}
