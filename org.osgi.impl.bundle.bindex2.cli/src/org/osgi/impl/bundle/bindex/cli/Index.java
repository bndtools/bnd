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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.launch.Framework;
import org.osgi.service.indexer.ResourceIndexer;
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
			run(args, index);
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		// We really need to ensure all non-daemon threads -- which may have been started by a bundle -- are terminated.
		System.exit(0);
	}
	
	private static void run(String[] args, ResourceIndexer index) {
		File output = new File(DEFAULT_FILENAME_COMPRESSED);
		Set<File> fileList = new HashSet<File>();
		Map<String, String> config = new HashMap<String, String>();
		
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

		if (fileList.isEmpty())
			printUsage();
		else try {
			index.index(fileList, new FileOutputStream(output), config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printCopyright(PrintStream out) {
		out.println("Bindex2 | Resource Indexer v1.0");
		out.println("(c) 2012 OSGi, All Rights Reserved");
	}

	private static void printUsage() {
		System.err
				.println("Arguments:\n" //
						+ "  [-r index.xml(.gz)]                                              --> Output file name.\n" //
						+ "  [--pretty                                                        --> Non-compressed, indented output.\n" //
						+ "  [-n Untitled]                                                    --> Repository name.\n"
						+ "  [-t \"%s\" symbolic name \"%v\" version \"%f\" filename \"%p\" dirpath ] --> Resource URL template.\n" //
						+ "  [-d rootdir]                                                     --> Root directory.\n" //
						+ "  [-h]                                                             --> Show help.\n" //
						+ "  [-l file:license.html]                                           --> Licence file.\n" //
						+ "  [-v]                                                             --> Verbose reporting.\n" //
						+ "  [-stylesheet "
						+ ResourceIndexer.STYLESHEET_DEFAULT + "]               --> Stylesheet URL.\n" //
						+ "  <file> [<file>*]");
	}
}
