/*
 * Copyright (c) OSGi Alliance (2004, 2017). All Rights Reserved.
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

package aQute.bnd.component;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines standard names for Service Component constants.
 *
 * @author $Id$
 */
@ProviderType
public interface ComponentConstants {
	/**
	 * Manifest header specifying the XML documents within a bundle that contain
	 * the bundle's Service Component descriptions.
	 * <p>
	 * The attribute value may be retrieved from the {@code Dictionary} object
	 * returned by the {@code Bundle.getHeaders} method.
	 */
	String	SERVICE_COMPONENT							= "Service-Component";

	/**
	 * A component property for a component configuration that contains the name
	 * of the component as specified in the {@code name} attribute of the
	 * {@code component} element. The value of this property must be of type
	 * {@code String}.
	 */
	String	COMPONENT_NAME								= "component.name";

	/**
	 * A component property that contains the generated id for a component
	 * configuration. The value of this property must be of type {@code Long}.
	 * <p>
	 * The value of this property is assigned by Service Component Runtime when
	 * a component configuration is created. Service Component Runtime assigns a
	 * unique value that is larger than all previously assigned values since
	 * Service Component Runtime was started. These values are NOT persistent
	 * across restarts of Service Component Runtime.
	 */
	String	COMPONENT_ID								= "component.id";

	/**
	 * A service registration property for a Component Factory that contains the
	 * value of the {@code factory} attribute. The value of this property must
	 * be of type {@code String}.
	 */
	String	COMPONENT_FACTORY							= "component.factory";

	/**
	 * The suffix for reference target properties. These properties contain the
	 * filter to select the target services for a reference. The value of this
	 * property must be of type {@code String}.
	 */
	String	REFERENCE_TARGET_SUFFIX						= ".target";

	/**
	 * The reason the component configuration was deactivated is unspecified.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_UNSPECIFIED				= 0;

	/**
	 * The component configuration was deactivated because the component was
	 * disabled.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_DISABLED				= 1;

	/**
	 * The component configuration was deactivated because a reference became
	 * unsatisfied.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_REFERENCE				= 2;

	/**
	 * The component configuration was deactivated because its configuration was
	 * changed.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_CONFIGURATION_MODIFIED	= 3;

	/**
	 * The component configuration was deactivated because its configuration was
	 * deleted.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_CONFIGURATION_DELETED	= 4;

	/**
	 * The component configuration was deactivated because the component was
	 * disposed.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_DISPOSED				= 5;

	/**
	 * The component configuration was deactivated because the bundle was
	 * stopped.
	 *
	 * @since 1.1
	 */
	int		DEACTIVATION_REASON_BUNDLE_STOPPED			= 6;

	/**
	 * Capability name for Service Component Runtime.
	 * <p>
	 * Used in {@code Provide-Capability} and {@code Require-Capability}
	 * manifest headers with the {@code osgi.extender} namespace. For example:
	 *
	 * <pre>
	 * Require-Capability: osgi.extender;
	 *  filter:="(&amp;(osgi.extender=osgi.component)(version&gt;=1.4)(!(version&gt;=2.0)))"
	 * </pre>
	 *
	 * @since 1.3
	 */
	String	COMPONENT_CAPABILITY_NAME					= "osgi.component";

	/**
	 * Compile time constant for the Specification Version of Declarative
	 * Services.
	 * <p>
	 * Used in {@code Version} and {@code Requirement} annotations. The value of
	 * this compile time constant will change when the specification version of
	 * Declarative Services is updated.
	 *
	 * @since 1.4
	 */
	String	COMPONENT_SPECIFICATION_VERSION				= "1.4.0";
}
