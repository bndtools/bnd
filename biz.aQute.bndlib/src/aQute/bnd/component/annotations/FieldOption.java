/*
 * Copyright (c) OSGi Alliance (2014, 2016). All Rights Reserved.
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
 * Field options for the {@link Reference} annotation.
 * 
 * @since 1.3
 * @author $Id$
 */
public enum FieldOption {
	
	/**
	 * The update field option is used to update the collection referenced by
	 * the field when there are changes to the bound services.
	 * 
	 * <p>
	 * This field option can only be used when the field reference has dynamic
	 * policy and multiple cardinality.
	 */
	UPDATE("update"),
	
	/**
	 * The replace field option is used to replace the field value with a new
	 * value when there are changes to the bound services.
	 */
	REPLACE("replace");

	private final String	value;

	FieldOption(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
