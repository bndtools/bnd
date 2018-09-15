/*
 * Copyright (c) OSGi Alliance (2011, 2016). All Rights Reserved.
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

package aQute.bnd.component.annotations;

/**
 * Configuration Policy for the {@link Component} annotation.
 * 
 * <p>
 * Controls whether component configurations must be satisfied depending on the
 * presence of a corresponding Configuration object in the OSGi Configuration
 * Admin service. A corresponding configuration is a Configuration object where
 * the PID is the name of the component.
 * 
 * @author $Id$
 * @since 1.1
 */
public enum ConfigurationPolicy {
	/**
	 * Use the corresponding Configuration object if present but allow the
	 * component to be satisfied even if the corresponding Configuration object
	 * is not present.
	 */
	OPTIONAL("optional"),

	/**
	 * There must be a corresponding Configuration object for the component
	 * configuration to become satisfied.
	 */
	REQUIRE("require"),

	/**
	 * Always allow the component configuration to be satisfied and do not use
	 * the corresponding Configuration object even if it is present.
	 */
	IGNORE("ignore");

	private final String	value;

	ConfigurationPolicy(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
