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
package bndtools.views.impexp;

import java.util.Map;
import java.util.Set;

import bndtools.editor.model.HeaderClause;


class ExportPackage extends HeaderClause {

	private final Set<String> uses;

	public ExportPackage(String name, Map<String, String> attribs, Set<String> uses) {
		super(name, attribs);
		this.uses = uses;
	}
	
	public Set<String> getUses() {
		return uses;
	}

}
