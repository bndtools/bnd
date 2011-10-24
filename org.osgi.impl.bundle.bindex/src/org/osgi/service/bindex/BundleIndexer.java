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

package org.osgi.service.bindex;

import java.util.Set;
import java.util.Map;
import java.io.File;
import java.io.OutputStream;

/**
 * The BundleIndexer is an OSGi service for indexing bundle capabiilities 
 * and requirements and create an OBR XML representation.
 * 
 * @version $Revision$
 */
public interface BundleIndexer {
  static final String REPOSITORY_NAME = "repository.name";
  static final String STYLESHEET = "stylesheet";
  static final String URL_TEMPLATE = "url.template";
  static final String ROOT_URL = "root.url";
  static final String LICENSE_URL = "license.url";

  /**
   * Index the input files and write the result to the given OutputStream
   * @param jarFiles a set of input jar files or directories
   * @param out the OutputStream to write to
   * @param config a set of optional parameters (use constants of this interface as keys)
   */
  void index(Set<File> jarFiles, OutputStream out, Map<String, String> config) throws Exception;
}
