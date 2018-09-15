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
 * Policy option for the {@link Reference} annotation.
 * 
 * @author $Id$
 * @since 1.2
 */
public enum ReferencePolicyOption {
	/**
	 * The reluctant policy option is the default policy option for both
	 * {@link ReferencePolicy#STATIC static} and {@link ReferencePolicy#DYNAMIC
	 * dynamic} reference policies. When a new target service for a reference
	 * becomes available, references having the reluctant policy option for the
	 * static policy or the dynamic policy with a unary cardinality will ignore
	 * the new target service. References having the dynamic policy with a
	 * multiple cardinality will bind the new target service.
	 */
	RELUCTANT("reluctant"),

	/**
	 * The greedy policy option is a valid policy option for both
	 * {@link ReferencePolicy#STATIC static} and {@link ReferencePolicy#DYNAMIC
	 * dynamic} reference policies. When a new target service for a reference
	 * becomes available, references having the greedy policy option will bind
	 * the new target service.
	 */
	GREEDY("greedy");

	private final String	value;

	ReferencePolicyOption(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
