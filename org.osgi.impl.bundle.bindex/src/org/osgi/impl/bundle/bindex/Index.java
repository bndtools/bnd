/*
 * $Id$
 * 
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
package org.osgi.impl.bundle.bindex;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import org.osgi.impl.bundle.obr.resource.*;

/**
 * Iterate over a set of given bundles and convert them to resources. After
 * this, convert an local urls (file systems, JAR file) to relative URLs and
 * create a ZIP file with the complete content. This ZIP file can be used in an
 * OSGi Framework to map to an http service or it can be expanded on the web
 * server's file system.
 * 
 * @version $Revision$
 */
public class Index {
	RepositoryImpl repository;
	String name = "Untitled";
	boolean quiet = false;
	URL root;
	String urlTemplate = null;

	String repositoryFileName = "repository.xml";
	URL licenseURL = null;
	boolean ignoreFlag = false;
	String stylesheet = "http://www.osgi.org/www/obr2html.xsl";

	/**
	 * Main entry. See -help for options.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Index index = new Index();
		index.run(args);
	}

	protected void run(String args[]) throws Exception {
		System.err.println("Bundle Indexer | v2.2");
		System.err.println("(c) 2007 OSGi, All Rights Reserved");

		Set<ResourceImpl> resources = new HashSet<ResourceImpl>();
		root = new File("").getAbsoluteFile().toURI().toURL();
		repository = new RepositoryImpl(root);

		for (int i = 0; i < args.length; i++)
			try {
				if (args[i].startsWith("-n"))
					name = args[++i];
				else if (args[i].equals("-stylesheet")) {
					stylesheet = args[++i];
				} else if (args[i].startsWith("-r")) {
					repositoryFileName = args[++i];
					repository = new RepositoryImpl(
							new File(repositoryFileName).getAbsoluteFile()
									.toURI().toURL());
				} else if (args[i].startsWith("-q"))
					quiet = true;
				else if (args[i].startsWith("-d")) {
					root = new File(args[++i]).toURI().toURL();
				} else if (args[i].startsWith("-t"))
					urlTemplate = args[++i];
				else if (args[i].startsWith("-l")) {
					licenseURL = new URL(new File("").toURI().toURL(),
							args[++i]);
				} else if (args[i].startsWith("-i")) {
					ignoreFlag = true;
				} else if (args[i].startsWith("-help")) {
					System.err
							.println("bindex " //
									+ "[-t \"%s\" symbolic name \"%v\" version \"%f\" filename \"%p\" dirpath ]\n" //
									+ "[-d rootFile]\n" //
									+ "[ -r repository.(xml|zip) ]\n" //
									+ "[-help]\n" //
									+ "[-ignore] #ignore exceptions when no manifest\n" //
									+ "[-l file:license.html ]\n" //
									+ "[-quiet]\n" //
									+ "[-stylesheet " + stylesheet + "  ]\n" //
									+ "<jar file>*");
				} else {
					recurse(resources, new File(args[i]));
				}
			} catch (Exception e) {
				System.err.println("Error in " + args[i] + " : "
						+ e.getMessage());
				e.printStackTrace();
			}

		List<ResourceImpl> sorted = new ArrayList<ResourceImpl>(resources);
		Collections.sort(sorted, new Comparator<ResourceImpl>() {
			public int compare(ResourceImpl r1, ResourceImpl r2) {
				String s1 = getName((ResourceImpl) r1);
				String s2 = getName((ResourceImpl) r2);
				return s1.compareTo(s2);
			}
		});

		Tag tag = doIndex(sorted);
		if (repositoryFileName != null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
					"UTF-8"));

			pw.println("<?xml version='1.0' encoding='utf-8'?>");
			pw.println("<?xml-stylesheet type='text/xsl' href='" + stylesheet
					+ "'?>");

			tag.print(0, pw);
			pw.close();
			byte buffer[] = out.toByteArray();
			String name = "repository.xml";
			FileOutputStream fout = new FileOutputStream(repositoryFileName);

			if (repositoryFileName.endsWith(".zip")) {
				ZipOutputStream zip = new ZipOutputStream(fout);
				CRC32 checksum = new CRC32();
				checksum.update(buffer);
				ZipEntry ze = new ZipEntry(name);
				ze.setSize(buffer.length);
				ze.setCrc(checksum.getValue());
				zip.putNextEntry(ze);
				zip.write(buffer, 0, buffer.length);
				zip.closeEntry();
				zip.close();
			} else {
				fout.write(buffer);
			}
			fout.close();
		}

		if (!quiet) {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
			pw.println("<?xml version='1.0' encoding='utf-8'?>");
			pw.println("<?xml-stylesheet type='text/xsl' href='" + stylesheet
					+ "'?>");
			tag.print(0, pw);
			pw.close();
		}
	}

	String getName(ResourceImpl impl) {
		String s = impl.getSymbolicName();
		if (s != null)
			return s;
		else {
			return "no-symbolic-name";
		}
	}

	void recurse(Set<ResourceImpl> resources, File path) throws Exception {
		if (path.isDirectory()) {
			String list[] = path.list();
			for (int i = 0; i < list.length; i++) {
				recurse(resources, new File(path, list[i]));
			}
		} else {
			if (path.getName().endsWith("ar")) { // ARJUN PATCH.jar")) {
				BundleInfo info;
				try {
					info = new BundleInfo(repository, path);
					ResourceImpl resource = info.build();
					if (urlTemplate != null) {
						doTemplate(path, resource);
					} else
						resource.setURL(path.toURI().toURL());

					resources.add(resource);
				} catch (Exception e) {
					if (ignoreFlag == false) {
						throw e;
					} else {
						System.err.println("Ignoring: " + path.getName()
								+ " with exception " + e.getMessage());
					}

				}
			}
		}
	}

	void doTemplate(File path, ResourceImpl resource)
			throws MalformedURLException {
		String dir = path.getAbsoluteFile().getParentFile().getAbsoluteFile()
				.toURI().toURL().toString();
		if (dir.endsWith("/"))
			dir = dir.substring(0, dir.length() - 1);

		if (dir.startsWith(root.toString()))
			dir = dir.substring(root.toString().length());

		String url = urlTemplate.replaceAll("%v", "" + resource.getVersion());
		url = url.replaceAll("%s", resource.getSymbolicName());
		url = url.replaceAll("%f", path.getName());
		url = url.replaceAll("%p", dir);
		resource.setURL(new URL(url));
	}

	/**
	 * Create the repository index
	 * 
	 * @param resources
	 *            Set of resources
	 * @param collected
	 *            The output zip file
	 * @throws IOException
	 */
	Tag doIndex(Collection<ResourceImpl> resources) throws IOException {
		Tag repository = new Tag("repository");
		repository.addAttribute("lastmodified", new Date());
		repository.addAttribute("name", name);

		for (ResourceImpl resource : resources) {
			repository.addContent(resource.toXML());
		}
		return repository;
	}

	/**
	 * Add the resource to the ZIP file, calculating the CRC etc.
	 * 
	 * @param zip
	 *            The output ZIP file
	 * @param name
	 *            The name of the resource
	 * @param actual
	 *            The contents stream
	 * @throws IOException
	 */
	void addToZip(ZipOutputStream zip, String name, InputStream actual)
			throws IOException {
		byte buffer[];
		buffer = readAll(actual, 0);
		actual.close();
		CRC32 checksum = new CRC32();
		checksum.update(buffer);
		ZipEntry ze = new ZipEntry(name);
		ze.setSize(buffer.length);
		ze.setCrc(checksum.getValue());
		zip.putNextEntry(ze);
		zip.write(buffer, 0, buffer.length);
		zip.closeEntry();
	}

	/**
	 * Read a complete stream till EOF. This method will parse the input stream
	 * until a -1 is discovered.
	 * 
	 * The method is recursive. It keeps on calling a higher level routine until
	 * EOF. Only then is the result buffer calculated.
	 */
	byte[] readAll(InputStream in, int offset) throws IOException {
		byte temp[] = new byte[4096];
		byte result[];
		int size = in.read(temp, 0, temp.length);
		if (size <= 0)
			return new byte[offset];
		//
		// We have a positive result, copy it
		// to the right offset.
		//
		result = readAll(in, offset + size);
		System.arraycopy(temp, 0, result, offset, size);
		return result;
	}
}
