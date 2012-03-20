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
package bndtools.model.importanalysis;

import java.util.Collection;
import java.util.Map;

import aQute.lib.osgi.Clazz;
import aQute.libg.header.Attrs;
import bndtools.model.clauses.HeaderClause;

public class ImportPackage extends HeaderClause {

	private final Collection<String> usedBy;
	private final Map<String, ? extends Collection<Clazz>> classes;
	private final boolean selfImport;

	public ImportPackage(String name, boolean selfImport, Attrs attribs, Collection<String> usedBy, Map<String, ? extends Collection<Clazz>> classes) {
		super(name, attribs);
		this.selfImport = selfImport;
		this.usedBy = usedBy;
		this.classes = classes;
	}
	public boolean isSelfImport() {
		return selfImport;
	}
	public Collection<String> getUsedBy() {
		return usedBy;
	};

	public Collection<Clazz> getImportingClasses(String importingPackage) {
		return classes.get(importingPackage);
	}
}
