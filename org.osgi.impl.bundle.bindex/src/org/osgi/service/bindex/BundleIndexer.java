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

package org.osgi.service.bindex;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * BundleIndexer is an OSGi service that creates an OBR XML representation by
 * indexing bundle capabilities and requirements.
 */
public interface BundleIndexer {
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

	/**
	 * Index a set of input files (bundles/jars) and/or directories, and write
	 * the OBR XML representation to the given OutputStream
	 * 
	 * @param jarFiles
	 *            a set of input files (bundles/jars) and/or directories
	 * @param out
	 *            the OutputStream to write the OBR XML representation to
	 * @param config
	 *            a set of optional parameters (use the interface constants as
	 *            keys)
	 */
	public void index(Set<File> jarFiles, OutputStream out,
			Map<String, String> config) throws Exception;
}
