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
import java.util.Map;
import java.util.Set;

/**
 * ResourceIndexer is an OSGi service that creates a Repository XML
 * representation by indexing resource capabilities and requirements.
 */
public interface ResourceIndexer {

	/** enable pretty-printing: non-gzipped, indented XML */
	public static final String PRETTY = "pretty";

	/** the default repository name */
	public static final String REPOSITORYNAME_DEFAULT = "Untitled";

	/** the name of the OBR XML representation */
	public static final String REPOSITORY_NAME = "repository.name";

	/** the default stylesheet for the OBR XML representation */
	public static final String STYLESHEET_DEFAULT = "http://www.osgi.org/www/obr2html.xsl";

	/** the stylesheet of the OBR XML representation */
	public static final String STYLESHEET = "stylesheet";

	/**
	 * Template for the URLs in the OBR XML representation. It can contain the
	 * following symbols:
	 * <ul>
	 * <li>%s is the symbolic name</li>
	 * <li>%v is the version number</li>
	 * <li>%f is the filename</li>
	 * <li>%p is the directory path</li>
	 * </ul>
	 */
	public static final String URL_TEMPLATE = "url.template";

	/** the root (directory) URL of the OBR */
	public static final String ROOT_URL = "root.url";

	/** the license URL of the OBR XML representation */
	public static final String LICENSE_URL = "license.url";

	public static final String VERBOSE = "verbose";


	/**
	 * Index a set of input files and write the Repository XML representation to
	 * the given writer
	 * 
	 * @param files
	 *            a set of input files
	 * @param out
	 *            the OutputStream to write the OBR XML representation
	 * @param config
	 *            a set of optional parameters (use the interface constants as
	 *            keys)
	 * @throws Exception
	 */
	void index(Set<File> files, OutputStream out, Map<String, String> config) throws Exception;

	/**
	 * Index a set of input files and write a Repository XML fragment to the
	 * given writer. Note that the result will be one or more XML
	 * <code>resource</code> elements <em>without</em> a top-level surrounding
	 * <code>repository</code> element. The resulting XML is therefore not
	 * well-formed. This method may be useful for repository managers that wish
	 * to (re-)index individual resources and assemble the XML fragments into a
	 * complete repository document later.
	 * 
	 * @param files
	 *            a set of input files
	 * @param out
	 *            the Writer to write the Repository XML representation
	 * @param config
	 *            a set of optional parameter (use the interface constants as
	 *            keys)
	 * @throws Exception
	 */
	void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception;
}
