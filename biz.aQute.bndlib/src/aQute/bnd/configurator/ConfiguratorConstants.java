/*
 * Copyright (c) OSGi Alliance (2017, 2018). All Rights Reserved.
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

package aQute.bnd.configurator;

/**
 * Defines standard constants for the Configurator services.
 * 
 * @author $Id$
 */
public final class ConfiguratorConstants {
	private ConfiguratorConstants() {
		// non-instantiable
	}

	/**
	 * Framework property specifying initial configurations to be applied by the
	 * Configurator on startup.
	 * <p>
	 * If the value of this property starts with a '{' (ignoring leading
	 * whitespace) it is interpreted as JSON and directly feed into the
	 * Configurator.
	 * <p>
	 * Otherwise the value is interpreted as a comma separated list of URLs
	 * pointing to JSON documents.
	 */
	public static final String	CONFIGURATOR_INITIAL	= "configurator.initial";

	/**
	 * Framework property specifying the directory to be used by the
	 * Configurator to store binary files.
	 * <p>
	 * If a value is specified, the Configurator will write all binaries to the
	 * given directory. Therefore the Configurator bundle needs read and write
	 * access to this directory.
	 * <p>
	 * If this property is not specified, the Configurator will store all binary
	 * files in its bundle private data area.
	 */
	public static final String	CONFIGURATOR_BINARIES	= "configurator.binaries";

	/**
	 * Prefix to mark properties as input for the Configurator when processing a
	 * configuration resource.
	 */
	public static final String	PROPERTY_PREFIX			= ":configurator:";

	/**
	 * Global property in the configuration resource specifying the version of
	 * the resource format.
	 * <p>
	 * Currently only version {@code 1} is defined for the JSON format and
	 * therefore the only allowed value is {@code 1} for this property. If this
	 * property is not specified, {@code 1} is assumed.
	 */
	public static final String	PROPERTY_RESOURCE_VERSION	= PROPERTY_PREFIX
			+ "resource-version";

	/**
	 * Global property in the configuration resource specifying the symbolic
	 * name of the configuration resource. If not specified the symbolic name of
	 * the bundle containing the resource is used. Mandatory for configuration
	 * resources that do not reside in a bundle
	 */
	public static final String	PROPERTY_SYMBOLIC_NAME		= PROPERTY_PREFIX
			+ "symbolic-name";

	/**
	 * Global property in the configuration resource specifying the version of
	 * the resource. If not specified the version of the bundle containing the
	 * resource is used. Mandatory for configuration resources that do not
	 * reside in a bundle.
	 */
	public static final String	PROPERTY_VERSION			= PROPERTY_PREFIX
			+ "version";

	/**
	 * Configuration property for the configuration ranking.
	 * <p>
	 * The value of this property must be convertible to a number.
	 */
	public static final String	PROPERTY_RANKING		= PROPERTY_PREFIX
			+ "ranking";

	/**
	 * Configuration property for the configuration policy.
	 * <p>
	 * Allowed values are {@link #POLICY_DEFAULT} and {@link #POLICY_FORCE}
	 * 
	 * @see #POLICY_DEFAULT
	 * @see #POLICY_FORCE
	 */
	public static final String	PROPERTY_POLICY			= PROPERTY_PREFIX
			+ "policy";

	/**
	 * Value for defining the default policy.
	 * 
	 * @see #PROPERTY_POLICY
	 */
	public static final String	POLICY_DEFAULT			= "default";

	/**
	 * Value for defining the force policy.
	 * 
	 * @see #PROPERTY_POLICY
	 */
	public static final String	POLICY_FORCE			= "force";

	/**
	 * The name of the extender capability attribute for the Configurator
	 */
	public static final String	CONFIGURATOR_EXTENDER_NAME			= "osgi.configurator";

	/**
	 * The version of the extender capability for the Configurator specification
	 */
	public static final String	CONFIGURATOR_SPECIFICATION_VERSION	= "1.0";
}