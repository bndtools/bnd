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
 * Service scope for the {@link Component} annotation.
 * 
 * @author $Id$
 * @since 1.3
 */
public enum ServiceScope {
	/**
	 * When the component is registered as a service, it must be registered as a
	 * bundle scope service but only a single instance of the component must be
	 * used for all bundles using the service.
	 */
	SINGLETON("singleton"),

	/**
	 * When the component is registered as a service, it must be registered as a
	 * bundle scope service and an instance of the component must be created for
	 * each bundle using the service.
	 */
	BUNDLE("bundle"),

	/**
	 * When the component is registered as a service, it must be registered as a
	 * prototype scope service and an instance of the component must be created
	 * for each distinct request for the service.
	 */
	PROTOTYPE("prototype"),

	/**
	 * Default element value for annotation. This is used to distinguish the
	 * default value for an element and should not otherwise be used.
	 */
	DEFAULT("<<default>>");

	private final String	value;

	ServiceScope(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
