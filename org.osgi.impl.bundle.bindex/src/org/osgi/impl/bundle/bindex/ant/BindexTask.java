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
package org.osgi.impl.bundle.bindex.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.osgi.impl.bundle.bindex.Indexer;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;

public class BindexTask extends Task {
	private Indexer indexer = new Indexer();

	public void setName(String name) {
		indexer.setName(name);
	}

	public void setQuiet(boolean quiet) {
		indexer.setQuiet(quiet);
	}

	public void setUrlTemplate(String urlTemplate) {
		indexer.setUrlTemplate(urlTemplate);
	}

	public void setLicenseURL(String license) throws MalformedURLException {
		indexer.setLicenseURL(license);
	}

	public void setStylesheet(String stylesheet) {
		indexer.setStylesheet(stylesheet);
	}

	public void setRootURL(String root) throws MalformedURLException {
		indexer.setRootURL(root);
	}

	public void setRepository(RepositoryImpl repository) {
		indexer.setRepository(repository);
	}

	public void setRepositoryFile(String repositoryFile) {
		indexer.setRepositoryFile(new File(repositoryFile));
	}

	private List<FileSet> filesets = new LinkedList<FileSet>(); // mandatory

	public void addFileset(FileSet fs) {
		filesets.add(fs);
	}

	/**
	 * Main entry. See -help for options.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void execute() throws BuildException {
		System.err.println("Bundle Indexer | v2.2");
		System.err.println("(c) 2007 OSGi, All Rights Reserved");

		List<File> fileList = new ArrayList<File>();
		for (FileSet fs : filesets) {
			DirectoryScanner ds = fs.getDirectoryScanner(getProject());
			File basedir = ds.getBasedir();
			String[] files = ds.getIncludedFiles();
			for (int i = 0; i < files.length; i++)
				fileList.add(new File(basedir, files[i]));
		}

		try {
			indexer.run(fileList);
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
}
