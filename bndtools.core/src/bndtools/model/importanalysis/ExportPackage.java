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

import java.util.List;

import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.libg.header.Attrs;
import bndtools.model.clauses.HeaderClause;

public class ExportPackage extends HeaderClause {

    private final List<PackageRef> uses;

    public ExportPackage(String name, Attrs attribs, List<PackageRef> uses) {
        super(name, attribs);
        this.uses = uses;
    }

    public List<PackageRef> getUses() {
        return uses;
    }

}
