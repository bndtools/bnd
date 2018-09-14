/*
 * Copyright (c) OSGi Alliance (2013, 2016). All Rights Reserved.
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
 * Reference scope for the {@link Reference} annotation.
 * 
 * @author $Id$
 * @since 1.3
 */
public enum ReferenceScope {
	/**
	 * A single service object is used for all references to the service in this
	 * bundle.
	 */
	BUNDLE("bundle"),

	/**
	 * If the bound service has prototype service scope, then each instance of
	 * the component with this reference can receive a unique instance of the
	 * service. If the bound service does not have prototype service scope, then
	 * this reference scope behaves the same as {@link #BUNDLE}.
	 */
	PROTOTYPE("prototype"),

	/**
	 * Bound services must have prototype service scope. Each instance of the
	 * component with this reference can receive a unique instance of the
	 * service.
	 */
	PROTOTYPE_REQUIRED("prototype_required");

	private final String	value;

	ReferenceScope(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
