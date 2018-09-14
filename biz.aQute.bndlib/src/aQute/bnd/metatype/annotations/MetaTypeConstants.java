/*
 * Copyright (c) OSGi Alliance (2005, 2017). All Rights Reserved.
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

package aQute.bnd.metatype.annotations;

/**
 * The MetaType Service can be used to obtain meta type information for a
 * bundle. The MetaType Service will examine the specified bundle for meta type
 * documents to create the returned {@code MetaTypeInformation} object.
 * 
 * <p>
 * If the specified bundle does not contain any meta type documents, then a
 * {@code MetaTypeInformation} object will be returned that wrappers any
 * {@code ManagedService} or {@code ManagedServiceFactory} services registered
 * by the specified bundle that implement {@code MetaTypeProvider}. Thus the
 * MetaType Service can be used to retrieve meta type information for bundles
 * which contain a meta type documents or which provide their own
 * {@code MetaTypeProvider} objects.
 * 
 * @ThreadSafe
 * @author $Id$
 * @since 1.1
 */
class MetaTypeConstants {

	/**
	 * Location of meta type documents. The MetaType Service will process each
	 * entry in the meta type documents directory.
	 */
	public final static String	METATYPE_DOCUMENTS_LOCATION	= "OSGI-INF/metatype";

	/**
	 * Capability name for meta type document processors.
	 * <p>
	 * Used in {@code Provide-Capability} and {@code Require-Capability}
	 * manifest headers with the {@code osgi.extender} namespace. For example:
	 * 
	 * <pre>
	 * Require-Capability: osgi.extender;
	 *  filter:="(&amp;(osgi.extender=osgi.metatype)(version&gt;=1.4)(!(version&gt;=2.0)))"
	 * </pre>
	 * 
	 * @since 1.3
	 */
	public static final String	METATYPE_CAPABILITY_NAME	= "osgi.metatype";

	/**
	 * Compile time constant for the Specification Version of MetaType Service.
	 * <p>
	 * Used in {@code Version} and {@code Requirement} annotations. The value of
	 * this compile time constant will change when the specification version of
	 * MetaType Service is updated.
	 * 
	 * @since 1.4
	 */
	public static final String	METATYPE_SPECIFICATION_VERSION	= "1.4.0";
}
