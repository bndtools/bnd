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

package aQute.bnd.metatype.annotations;

/**
 * Attribute types for the {@link AttributeDefinition} annotation.
 * 
 * @see AttributeDefinition#type()
 * @author $Id$
 */
public enum AttributeType {
	/**
	 * The {@code String} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code String},
	 * {@code List<String>} or {@code String[]} objects, depending on the
	 * {@link AttributeDefinition#cardinality() cardinality} value.
	 */
	STRING("String"),

	/**
	 * The {@code Long} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Long},
	 * {@code List<Long>} or {@code long[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	LONG("Long"),

	/**
	 * The {@code Integer} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Integer},
	 * {@code List<Integer>} or {@code int[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	INTEGER("Integer"),

	/**
	 * The {@code Short} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Short},
	 * {@code List<Short>} or {@code short[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	SHORT("Short"),

	/**
	 * The {@code Character} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Character},
	 * {@code List<Character>} or {@code char[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	CHARACTER("Character"),

	/**
	 * The {@code Byte} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Byte},
	 * {@code List<Byte>} or {@code byte[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	BYTE("Byte"),

	/**
	 * The {@code Double} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Double},
	 * {@code List<Double>} or {@code double[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	DOUBLE("Double"),

	/**
	 * The {@code Float} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Float},
	 * {@code List<Float>} or {@code float[]} objects, depending on the
	 * {@code AttributeDefinition#cardinality() cardinality} value.
	 */
	FLOAT("Float"),

	/**
	 * The {@code Boolean} type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code Boolean},
	 * {@code List<Boolean>} or {@code boolean[]} objects depending on
	 * {@code AttributeDefinition#cardinality() cardinality}.
	 */
	BOOLEAN("Boolean"),

	/**
	 * The {@code Password} type.
	 * 
	 * <p>
	 * Attributes of this type must be stored as {@code String},
	 * {@code List<String>} or {@code String[]} objects depending on
	 * {@link AttributeDefinition#cardinality() cardinality}.
	 * 
	 * <p>
	 * A {@code Password} must be treated as a {@code String} but the type can
	 * be used to disguise the information when displayed to a user to prevent
	 * it from being seen.
	 */
	PASSWORD("Password");

	private final String	value;

	AttributeType(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
