/*
 * Copyright (c) OSGi Alliance (2011, 2017). All Rights Reserved.
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
 * Policy for the {@link Reference} annotation.
 * 
 * @author $Id$
 */
public enum ReferencePolicy {
	/**
	 * The static policy is the most simple policy and is the default policy. A
	 * component instance never sees any of the dynamics. Component
	 * configurations are deactivated before any bound service for a reference
	 * having a static policy becomes unavailable. If a target service is
	 * available to replace the bound service which became unavailable, the
	 * component configuration must be reactivated and bound to the replacement
	 * service.
	 */
	STATIC("static"),

	/**
	 * The dynamic policy is slightly more complex since the component
	 * implementation must properly handle changes in the set of bound services.
	 * With the dynamic policy, SCR can change the set of bound services without
	 * deactivating a component configuration. If the component uses method
	 * injection to access services, then the component instance will be
	 * notified of changes in the set of bound services by calls to the bind and
	 * unbind methods.
	 */
	DYNAMIC("dynamic");

	private final String	value;

	ReferencePolicy(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
