/*
 * $Id: BindexTask.java 44 2007-07-13 20:49:41Z hargrave@us.ibm.com $
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
package org.osgi.impl.bundle.bindex.ant;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;
import org.osgi.impl.bundle.obr.resource.*;


/**
 * This Ant task is based on the http://bundles.osgi.org/build/jar/bindex.jar
 * source code
 * 
 * <p>
 * Iterate over a set of given bundles and convert them to resources. After
 * this, convert an local urls (file systems, JAR file) to relative URLs and
 * create a ZIP file with the complete content. This ZIP file can be used in an
 * OSGi Framework to map to an http service or it can be expanded on the web
 * server's file system.
 * 
 * @version $Revision: 44 $
 * 
 */
public class BindexTask extends Task {
	File			repositoryFile;										// optional
	String			license;												// optional
	boolean			quiet						= false;					// optional
	String			name						= "Untitled";				// optional

	/**
	 * template for the URL containing the following symbols
	 * <p>
	 * %s is the symbolic name
	 * <p>
	 * %v is the version number
	 * <p>
	 * %f is the filename
	 * <p>
	 * %p is the dir path
	 */
	String			urlTemplate					= null;					// optional
	File			rootFile					= new File("")
														.getAbsoluteFile(); // optional
	List<FileSet>	filesets	= new LinkedList<FileSet>();		// mandatory
	RepositoryImpl	repository;
	String			root;
	Set<ResourceImpl>	resources					= new HashSet<ResourceImpl>();

	public void setLicenseURL(String license) {
		this.license = license;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	public void setRepositoryFile(File repositoryFile) {
		this.repositoryFile = repositoryFile;
	}

	public void setRoot(File rootFile) {
		this.rootFile = rootFile;
	}

	public void setUrlTemplate(String urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	public void addFileset(FileSet fs) {
		filesets.add(fs);
	}

	/**
	 * Main entry. See -help for options.
	 * 
	 * @param args
	 * @throws Exception
	 * @throws Exception
	 */
	public void execute() throws BuildException {

		try {
			// Parameters setting section

			System.err.println("Bundle Indexer | v2.0");
			System.err.println("(c) 2005 OSGi, All Rights Reserved");

			// System.err.println("bindex [-o repository.zip] [-t \"%s\"
			// symbolic name \"%v\" version \"%f\" filename \"%p\" dirpath ] [
			// -r repository.xml ] [-help] [-l file:license.html ] [-qiueit]
			// <jar file>*");

			try {
				root = rootFile.toURI().toURL().toString();
			}
			catch (Exception e) {
				throw new BuildException(e + " for rootFile");
			}
			try {
				repository = new RepositoryImpl(rootFile.toURI().toURL());
			}
			catch (Exception e) {
				throw new BuildException(e + " for repo");
			}

			for (FileSet fs : filesets) {
				DirectoryScanner ds = fs.getDirectoryScanner(getProject());
				File basedir = ds.getBasedir();
				String[] files = ds.getIncludedFiles();
				for (int i = 0; i < files.length; i++)
					try {
						recurse(resources, new File(basedir, files[i]));
					}
					catch (Exception e) {
						throw new BuildException(e);
					}
			}

			// Execution section

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
				tag.print(0, pw);
				pw.close();
				byte buffer[] = out.toByteArray();
				String name = "repository.xml";
				FileOutputStream fout = new FileOutputStream(repositoryFile);

				if (repositoryFile.getAbsolutePath().endsWith(".zip")) {
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
				}
				else {
					fout.write(buffer);
				}
				fout.close();
			}

			if (!quiet) {
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(
						System.out));
				tag.print(0, pw);
				pw.close();
			}

		}
		catch (Exception e) {
			throw new BuildException(e);
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
		}
		else {
			if (path.getName().endsWith(".jar")) {
				// ADD by Didier
				log("process " + path + " ...");
				// END of ADD

				BundleInfo info = new BundleInfo(repository, path);
				ResourceImpl resource = info.build();
				if (urlTemplate != null) {
					doTemplate(path, resource);
				}
				else
					resource.setURL(path.toURI().toURL());

				resources.add(resource);
			}
		}
	}

	void doTemplate(File path, ResourceImpl resource)
			throws MalformedURLException {
		String dir = path.getParentFile().getAbsoluteFile().toURI().toURL().toString();
		if (dir.endsWith("/"))
			dir = dir.substring(0, dir.length() - 1);

		if (dir.startsWith(root))
			dir = dir.substring(root.length());

		String url = urlTemplate.replaceAll("%v", "" + resource.getVersion());
		url = url.replaceAll("%s", resource.getSymbolicName());
		url = url.replaceAll("%f", path.getName());
		url = url.replaceAll("%p", dir);
		resource.setURL(new URL(url));
	}

	/**
	 * Create the repository index
	 * 
	 * @param resources Set of resources
	 * @param collected The output zip file
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
	 * @param zip The output ZIP file
	 * @param name The name of the resource
	 * @param actual The contents stream
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
