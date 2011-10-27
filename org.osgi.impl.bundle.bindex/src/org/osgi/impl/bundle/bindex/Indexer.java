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
package org.osgi.impl.bundle.bindex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImplComparator;
import org.osgi.impl.bundle.obr.resource.Tag;

/**
 * Iterate over a set of given bundles and convert them to resources. After
 * this, convert any local urls (file systems, JAR file) to relative URLs and
 * (optionally) create a ZIP file with the complete content. This ZIP file can
 * be used in an OSGi Framework to map to an http service or it can be expanded
 * on the web server's file system.
 */
public class Indexer {
	/** the default repository name */
	static public final String REPOSITORYNAME_DEFAULT = "Untitled";

	private String repositoryName = REPOSITORYNAME_DEFAULT;

	/**
	 * @param repositoryName
	 *            the repository name
	 */
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	private boolean verbose = false;

	/**
	 * @param verbose
	 *            the repository inventory xml file will be output to stdout
	 *            when true
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private String urlTemplate = null;

	/**
	 * @param urlTemplate
	 *            Template for the URLs in the repository inventory xml file. It
	 *            can contain the following symbols:
	 *            <ul>
	 *            <li>%s is the symbolic name</li>
	 *            <li>%v is the version number</li>
	 *            <li>%f is the filename</li>
	 *            <li>%p is the directory path</li>
	 *            </ul>
	 */
	public void setUrlTemplate(String urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	/** the license URL for the repository */
	@SuppressWarnings("unused")
	private URL licenseURL = null;

	/**
	 * @param licenseURL
	 *            the license URL for the repository
	 * @throws MalformedURLException
	 *             when the license URL is not a valid URL string
	 */
	public void setLicenseURL(String licenseURL) throws MalformedURLException {
		this.licenseURL = new URL(licenseURL);
	}

	/** the default stylesheet for the repository inventory xml file */
	public static final String STYLESHEET_DEFAULT = "http://www.osgi.org/www/obr2html.xsl";

	private String stylesheet = STYLESHEET_DEFAULT;

	/**
	 * @param stylesheet
	 *            the stylesheet for the repository inventory xml file
	 */
	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}

	private URL rootURL = null;

	/**
	 * @param rootURL
	 *            the root directory URL of the repository
	 */
	public void setRootURL(URL rootURL) {
		this.rootURL = rootURL;
	}

	/**
	 * @param rootURL
	 *            the root directory URL of the repository
	 * @throws MalformedURLException
	 *             when the root URL is not a valid URL string
	 */
	public void setRootURL(String rootURL) throws MalformedURLException {
		this.rootURL = new URL(rootURL);
	}

	/**
	 * @return the root directory URL of the repository
	 */
	public URL getRootURL() {
		return rootURL;
	}

	private RepositoryImpl repository = null;

	/**
	 * @param repository
	 *            the repository
	 */
	public void setRepository(RepositoryImpl repository) {
		this.repository = repository;
	}

	private File repositoryFile = null;

	/**
	 * @param repositoryFile
	 *            the repository inventory file
	 */
	public void setRepositoryFile(File repositoryFile) {
		this.repositoryFile = repositoryFile;
	}

	/**
	 * Create the repository index from a collection of resources
	 * 
	 * @param resources
	 *            a collection of resources
	 * @return the repository index
	 */
	public Tag doIndex(Collection<ResourceImpl> resources) {
		Tag repository = new Tag("repository");
		repository.addAttribute("lastmodified", new Date());
		repository.addAttribute("name", repositoryName);

		for (ResourceImpl resource : resources) {
			repository.addContent(resource.toXML());
		}
		return repository;
	}

	/**
	 * Create the repository inventory xml file from a list of files
	 * 
	 * @param fileList
	 *            a list of files to iterate over. Each entry in the list will
	 *            be traversed recursively.
	 * @throws Exception
	 *             in case of an error
	 */
	public void run(List<File> fileList) throws Exception {
		Set<ResourceImpl> resources = new HashSet<ResourceImpl>();
		/*
		 * only set the root directory URL to the current directory when it
		 * wasn't set yet
		 */
		if (rootURL == null) {
			rootURL = new File("").getAbsoluteFile().toURI().toURL();
		}
		repository = new RepositoryImpl(rootURL);

		/* recurse over every entry in the list */
		for (File file : fileList) {
			recurse(resources, file);
		}

		List<ResourceImpl> sorted = new ArrayList<ResourceImpl>(resources);
		Collections.sort(sorted, new ResourceImplComparator());

		Tag tag = doIndex(sorted);

		/*
		 * only write out the repository inventory file when the repository file
		 * is configured
		 */
		if (repositoryFile != null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
					"UTF-8"));

			printXmlHeader(pw);

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

		/* only print out the repository inventory when verbose is configured */
		if (verbose) {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
			printXmlHeader(pw);
			tag.print(0, pw);
			pw.close();
		}
	}

	/**
	 * @param pw
	 *            the print writer to print the repository inventory xml file
	 *            header to
	 */
	public void printXmlHeader(PrintWriter pw) {
		pw.println("<?xml version='1.0' encoding='utf-8'?>");
		pw.println("<?xml-stylesheet type='text/xsl' href='" + stylesheet
				+ "'?>");
	}

	/**
	 * Recurse on path to find all bundles/jars and add their information to the
	 * set of resources. It recursively visits every file under path, determines
	 * whether it is a bundle/jar and if so then get its information.
	 * 
	 * @param resources
	 *            the set of resources to add bundle/jar information to
	 * @param path
	 *            the path to recurse on
	 * @throws Exception
	 *             in case of an error
	 */
	public void recurse(Set<ResourceImpl> resources, File path)
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

	/**
	 * @param out
	 *            the print stream to print the copyright message to
	 */
	public void printCopyright(PrintStream out) {
		out.println("Bundle Indexer | v3.0");
		out.println("(c) 2007 OSGi, All Rights Reserved");
	}

	/**
	 * Apply the URL template to a path, and set the resulting URL in resource
	 * 
	 * @param path
	 *            the path to apply the template to
	 * @param resource
	 *            the resource to set the resulting URL in
	 * @throws MalformedURLException
	 *             in case of an error
	 */
	private void doTemplate(File path, ResourceImpl resource)
			throws MalformedURLException {
		String dir = path.getAbsoluteFile().getParentFile().getAbsoluteFile()
				.toURI().toURL().toString();
		if (dir.endsWith("/"))
			dir = dir.substring(0, dir.length() - 1);

		if (dir.startsWith(rootURL.toString()))
			dir = dir.substring(rootURL.toString().length());

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
}
