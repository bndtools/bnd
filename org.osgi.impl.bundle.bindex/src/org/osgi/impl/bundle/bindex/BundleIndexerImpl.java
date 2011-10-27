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
import java.util.*;

import org.osgi.impl.bundle.obr.resource.*;
import org.osgi.service.bindex.*;

import aQute.bnd.annotation.component.*;

/**
 * BundleIndexer implementation based on Indexer
 * 
 * @version $Revision$
 */
@Component
public class BundleIndexerImpl extends Indexer implements BundleIndexer {

	public BundleIndexerImpl() {
		super();
		setRepositoryFile(new File("repository.xml"));
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
				setName(v);
			if ((v = config.get(STYLESHEET)) != null)
				setStylesheet(v);
			if ((v = config.get(URL_TEMPLATE)) != null)
				setUrlTemplate(v);
			if ((v = config.get(ROOT_URL)) != null)
				setRootURL(v);
			if ((v = config.get(LICENSE_URL)) != null)
				setLicenseURL(v);
		}

		if (getRoot() == null)
			setRootURL(new File("").getAbsoluteFile().toURI().toURL());
		setRepository(new RepositoryImpl(getRoot()));

		Set<ResourceImpl> resources = new HashSet<ResourceImpl>();
		for (File f : jarFiles)
			super.recurse(resources, f);

		List<ResourceImpl> sorted = new ArrayList<ResourceImpl>(resources);
		Collections.sort(sorted, new Comparator<ResourceImpl>() {
			public int compare(ResourceImpl r1, ResourceImpl r2) {
				String s1 = getName((ResourceImpl) r1);
				String s2 = getName((ResourceImpl) r2);
				return s1.compareTo(s2);
			}
		});

		Tag tag = super.doIndex(sorted);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

		try {
			printXmlHeader(pw);
			tag.print(0, pw);
		} finally {
			pw.close();
		}
	}
}
