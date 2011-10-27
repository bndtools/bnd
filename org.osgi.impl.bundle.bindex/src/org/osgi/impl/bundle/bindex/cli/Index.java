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
import java.util.ArrayList;
import java.util.List;

import org.osgi.impl.bundle.bindex.Indexer;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;

public class Index {
	/**
	 * Main entry. See -help for options.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Indexer index = new Indexer();
		index.printCopyright(System.err);
		index.setRepositoryFile(new File("repository.xml"));

		List<File> fileList = new ArrayList<File>();
		for (int i = 0; i < args.length; i++) {
			try {
				if (args[i].startsWith("-n"))
					index.setName(args[++i]);
				else if (args[i].equals("-stylesheet")) {
					index.setStylesheet(args[++i]);
				} else if (args[i].startsWith("-r")) {
					File repositoryFile = new File(args[++i]);
					index.setRepositoryFile(repositoryFile);
					index.setRepository(new RepositoryImpl(repositoryFile
							.getAbsoluteFile().toURI().toURL()));
				} else if (args[i].startsWith("-v"))
					index.setVerbose(true);
				else if (args[i].startsWith("-d")) {
					index.setRootURL(args[++i]);
				} else if (args[i].startsWith("-t"))
					index.setUrlTemplate(args[++i]);
				else if (args[i].startsWith("-l")) {
					index.setLicenseURL(args[++i]);
				} else if (args[i].startsWith("-h")) {
					System.err
							.println("bindex " //
									+ "[-t \"%s\" symbolic name \"%v\" version \"%f\" filename \"%p\" dirpath ]\n" //
									+ "[-d rootFile]\n" //
									+ "[-r repository.(xml|zip)]\n" //
									+ "[-h]\n" //
									+ "[-l file:license.html]\n" //
									+ "[-v]\n" //
									+ "[-stylesheet "
									+ Indexer.STYLESHEET_DEFAULT + "]\n" //
									+ "<jar file>*");
				} else {
					fileList.add(new File(args[i]));
				}
			} catch (Exception e) {
				System.err.println("Error in " + args[i] + " : "
						+ e.getMessage());
				e.printStackTrace();
			}
		}

		index.run(fileList);
	}
}
