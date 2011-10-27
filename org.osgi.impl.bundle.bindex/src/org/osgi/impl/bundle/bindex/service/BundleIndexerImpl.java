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

package org.osgi.impl.bundle.bindex.service;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.impl.bundle.bindex.Indexer;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImplComparator;
import org.osgi.impl.bundle.obr.resource.Tag;
import org.osgi.service.bindex.BundleIndexer;

import aQute.bnd.annotation.component.Component;

/**
 * BundleIndexer implementation based on Indexer
 */
@Component
public class BundleIndexerImpl implements BundleIndexer {
	private Indexer indexer = new Indexer();

	public BundleIndexerImpl() {
		super();
		indexer.setRepositoryFile(new File("repository.xml"));
	}

	OutputStream out;

	public synchronized void index(Set<File> jarFiles, OutputStream out,
			Map<String, String> config) throws Exception {
		if (jarFiles == null || jarFiles.isEmpty())
			throw new IllegalArgumentException("No input jar provided");
		if (out == null)
			throw new IllegalArgumentException("No output stream provided");
		this.out = out;

		if (config != null) {
			String v = null;
			if ((v = config.get(REPOSITORY_NAME)) != null)
				indexer.setRepositoryName(v);
			if ((v = config.get(STYLESHEET)) != null)
				indexer.setStylesheet(v);
			if ((v = config.get(URL_TEMPLATE)) != null)
				indexer.setUrlTemplate(v);
			if ((v = config.get(ROOT_URL)) != null)
				indexer.setRootURL(v);
			if ((v = config.get(LICENSE_URL)) != null)
				indexer.setLicenseURL(v);
		}

		if (indexer.getRootURL() == null)
			indexer.setRootURL(new File("").getAbsoluteFile().toURI().toURL());
		indexer.setRepository(new RepositoryImpl(indexer.getRootURL()));

		Set<ResourceImpl> resources = new HashSet<ResourceImpl>();
		for (File f : jarFiles)
			indexer.recurse(resources, f);

		List<ResourceImpl> sorted = new ArrayList<ResourceImpl>(resources);
		Collections.sort(sorted, new ResourceImplComparator());

		Tag tag = indexer.doIndex(sorted);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

		try {
			indexer.printXmlHeader(pw);
			tag.print(0, pw);
		} finally {
			pw.close();
		}
	}
}
