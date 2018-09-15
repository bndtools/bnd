/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
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
 * Collection types for the {@link Reference} annotation.
 * 
 * @since 1.4
 * @author $Id$
 */
public enum CollectionType {
	/**
	 * The service collection type is used to indicate the collection holds the
	 * bound service objects.
	 * <p>
	 * This is the default collection type.
	 */
	SERVICE("service"),

	/**
	 * The reference collection type is used to indicate the collection holds
	 * Service References for the bound services.
	 */
	REFERENCE("reference"),

	/**
	 * The serviceobjects collection type is used to indicate the collection
	 * holds Component Service Objects for the bound services.
	 */
	SERVICEOBJECTS("serviceobjects"),

	/**
	 * The properties collection type is used to indicate the collection holds
	 * unmodifiable Maps containing the service properties of the bound
	 * services.
	 * <p>
	 * The Maps must implement {@code Comparable} with the {@code compareTo}
	 * method comparing service property maps using the same ordering as
	 * {@code ServiceReference.compareTo} based upon service ranking and service
	 * id.
	 */
	PROPERTIES("properties"),

	/**
	 * The tuple collection type is used to indicate the collection holds
	 * unmodifiable Map.Entries whose key is an unmodifiable Map containing the
	 * service properties of the bound service, as specified in
	 * {@link #PROPERTIES}, and whose value is the bound service object.
	 * <p>
	 * The Map.Entries must implement {@code Comparable} with the
	 * {@code compareTo} method comparing service property maps using the same
	 * ordering as {@code ServiceReference.compareTo} based upon service ranking
	 * and service id.
	 */
	TUPLE("tuple");

	private final String value;

	CollectionType(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
