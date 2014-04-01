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

package org.osgi.service.indexer;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * ResourceIndexer is an OSGi service that creates a Repository XML
 * representation by indexing resource capabilities and requirements.
 */
@ProviderType
public interface ResourceIndexer {
	/**
	 * Name of the configuration variable to enable pretty-printing: indented
	 * XML
	 */
	public static final String PRETTY = "pretty";

	/**
	 * Name of the configuration variable to enable compression: gzipped XML
	 */
	public static final String COMPRESSED = "compressed";

	/** the default repository name */
	public static final String REPOSITORYNAME_DEFAULT = "Untitled";

	/** Name of the configuration variable for the repository name */
	public static final String REPOSITORY_NAME = "repository.name";

	/** the default stylesheet for the XML representation */
	public static final String STYLESHEET_DEFAULT = "http://www.osgi.org/www/obr2html.xsl";

	/**
	 * Name of the configuration variable for the stylesheet of the XML
	 * representation
	 */
	public static final String STYLESHEET = "stylesheet";

	/**
	 * Name of the configuration variable for the template for the URLs in the
	 * XML representation. A template can contain the following symbols:
	 * <ul>
	 * <li>%s is the symbolic name</li>
	 * <li>%v is the version number</li>
	 * <li>%f is the filename</li>
	 * <li>%p is the directory path</li>
	 * </ul>
	 */
	public static final String URL_TEMPLATE = "url.template";

	/**
	 * Name of the configuration variable for the root (directory) URL of the
	 * repository
	 */
	public static final String ROOT_URL = "root.url";

	/**
	 * Name of the configuration variable for the license URL of the repository
	 */
	public static final String LICENSE_URL = "license.url";

	/** Name of the configuration variable for the verbose mode */
	public static final String VERBOSE = "verbose";

	/**
	 * Index a set of input files and write the Repository XML representation to
	 * the stream
	 * 
	 * @param files
	 *            a set of input files
	 * @param out
	 *            the stream to write the XML representation to
	 * @param config
	 *            a set of optional parameters (use the interface constants as
	 *            keys)
	 * @throws Exception
	 *             in case of an error
	 */
	void index(Set<File> files, OutputStream out, Map<String, String> config) throws Exception;

	/**
	 * <p>
	 * Index a set of input files and write a Repository XML fragment to the
	 * given writer.
	 * </p>
	 * <p>
	 * Note that the result will be one or more XML <code>resource</code>
	 * elements <em>without</em> a top-level surrounding <code>repository</code>
	 * element. The resulting XML is therefore not well-formed.
	 * </p>
	 * <p>
	 * This method may be useful for repository managers that wish to (re-)index
	 * individual resources and assemble the XML fragments into a complete
	 * repository document later.
	 * </p>
	 * 
	 * @param files
	 *            a set of input files
	 * @param out
	 *            the writer to write the Repository XML representation to
	 * @param config
	 *            a set of optional parameter (use the interface constants as
	 *            keys)
	 * @throws Exception
	 *             in case of an error
	 */
	void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception;

	/**
	 * Return a Resource from a file
	 * 
	 * @param file
	 *            a bundle to index
	 * @return The resource, caps, and reqs for that file
	 */

	class IndexResult {
		public Resource resource;
		public List<Capability> capabilities = new ArrayList<Capability>();
		public List<Requirement> requirements = new ArrayList<Requirement>();

		/**
		 * A unique signature for this indexer. It should be some kind of hash
		 * that changes when the set of analyzers changes, or the results of
		 * this parse are no longer compatible with other parse results. The
		 * intention of this method is to allow caching of results and
		 * invalidate the cache when the version has changed.
		 */
		public long signature;
	}

	IndexResult indexFile(File file) throws Exception;
}
