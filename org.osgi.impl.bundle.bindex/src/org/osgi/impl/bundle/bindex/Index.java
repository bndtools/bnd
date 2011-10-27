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
	private String name = "Untitled";

	public void setName(String name) {
		this.name = name;
	}

	private boolean quiet = false;

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	private String urlTemplate = null;

	public void setUrlTemplate(String urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	@SuppressWarnings("unused")
	private URL licenseURL = null;

	public void setLicenseURL(String license) throws MalformedURLException {
		this.licenseURL = new URL(license);
	}

	private String stylesheet = "http://www.osgi.org/www/obr2html.xsl";

	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}

	public String getStylesheet() {
		return stylesheet;
	}

	private URL root = null;

	public void setRootURL(URL root) {
		this.root = root;
	}

	public void setRootURL(String root) throws MalformedURLException {
		this.root = new URL(root);
	}

	public URL getRoot() {
		return root;
	}

	private RepositoryImpl repository = null;

	public void setRepository(RepositoryImpl repository) {
		this.repository = repository;
	}

	private File repositoryFile = null;

	public void setRepositoryFile(File repositoryFile) {
		this.repositoryFile = repositoryFile;
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
	public Tag doIndex(Collection<ResourceImpl> resources) throws IOException {
		Tag repository = new Tag("repository");
		repository.addAttribute("lastmodified", new Date());
		repository.addAttribute("name", name);

		for (ResourceImpl resource : resources) {
			repository.addContent(resource.toXML());
		}
		return repository;
	}

	private void run(String args[]) throws Exception {
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
					repositoryFile = new File(args[++i]);
					repository = new RepositoryImpl(
							repositoryFile.getAbsoluteFile()
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
				} else if (args[i].startsWith("-help")) {
					System.err
							.println("bindex " //
									+ "[-t \"%s\" symbolic name \"%v\" version \"%f\" filename \"%p\" dirpath ]\n" //
									+ "[-d rootFile]\n" //
									+ "[ -r repository.(xml|zip) ]\n" //
									+ "[-help]\n" //
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
				String s1 = getName(r1);
				String s2 = getName(r2);
				return s1.compareTo(s2);
			}
		});

		Tag tag = doIndex(sorted);
		if (repositoryFile != null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
					"UTF-8"));

			pw.println("<?xml version='1.0' encoding='utf-8'?>");
			pw.println("<?xml-stylesheet type='text/xsl' href='" + stylesheet + "'?>");

			tag.print(0, pw);
			pw.close();
			byte buffer[] = out.toByteArray();
			String name = "repository.xml";
			FileOutputStream fout = new FileOutputStream(repositoryFile);

			if (repositoryFile.getAbsolutePath().endsWith(".zip")) {
				ZipOutputStream zip = new ZipOutputStream(fout);
				addToZip(zip, name, buffer);
				zip.close();
			} else {
				fout.write(buffer);
			}
			fout.close();
		}

		if (!quiet) {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
			pw.println("<?xml version='1.0' encoding='utf-8'?>");
			pw.println("<?xml-stylesheet type='text/xsl' href='" + stylesheet + "'?>");
			tag.print(0, pw);
			pw.close();
		}
	}

	protected String getName(ResourceImpl impl) {
		String s = impl.getSymbolicName();
		if (s != null)
			return s;
		else {
			return "no-symbolic-name";
		}
	}

	protected void recurse(Set<ResourceImpl> resources, File path)
			throws Exception {
		if (path.isDirectory()) {
			for (String pathEntry : path.list()) {
				recurse(resources, new File(path, pathEntry));
			}
		} else {
			BundleInfo info = null;
			try {
				info = new BundleInfo(repository, path);
			} catch (Exception e) {
				/* swallow: is not a bundle/jar */
			}
			if (info != null) {
				ResourceImpl resource = info.build();
				if (urlTemplate != null) {
					doTemplate(path, resource);
				} else {
					resource.setURL(path.toURI().toURL());
				}

				resources.add(resource);
			}
		}
	}

	private void doTemplate(File path, ResourceImpl resource)
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
	 * Add the resource to the ZIP file, calculating the CRC etc.
	 * 
	 * @param zip
	 *            The output ZIP file
	 * @param name
	 *            The name of the resource
	 * @param buffer
	 *            The buffer that contain the resource
	 * @throws IOException
	 */
	private void addToZip(ZipOutputStream zip, String name, byte[] buffer)
			throws IOException {
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
	 * Main entry. See -help for options.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		System.err.println("Bundle Indexer | v2.2");
		System.err.println("(c) 2007 OSGi, All Rights Reserved");

		Index index = new Index();
		index.setRepositoryFile(new File("repository.xml"));
		index.run(args);
	}
}
