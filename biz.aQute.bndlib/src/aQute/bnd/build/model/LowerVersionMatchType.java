/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package aQute.bnd.build.model;

public enum LowerVersionMatchType {
	Exact("${@}"), Micro("${version;===;${@}}"), Minor("${version;==;${@}}"), Major("${version;=;${@}}");

	private final String	representation;

	private LowerVersionMatchType(String representation) {
		this.representation = representation;
	}

	public String getRepresentation() {
		return representation;
	}

	public static LowerVersionMatchType parse(String string) throws IllegalArgumentException {
		for (LowerVersionMatchType type : LowerVersionMatchType.class.getEnumConstants()) {
			if (type.getRepresentation().equals(string)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Failed to parse version match type.");
	}
}
