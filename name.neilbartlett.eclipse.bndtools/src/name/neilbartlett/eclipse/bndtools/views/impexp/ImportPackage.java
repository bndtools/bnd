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
package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Collection;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

public class ImportPackage extends HeaderClause {

	private final Collection<? extends String> usedBy;

	public ImportPackage(String name, Map<String, String> attribs, Collection<? extends String> usedBy) {
		super(name, attribs);
		this.usedBy = usedBy;
	}
	
	public Collection<? extends String> getUsedBy() {
		return usedBy;
	};

}
