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
package bndtools.model.clauses;

import org.osgi.framework.Constants;

import aQute.libg.header.Attrs;

public class ExportedPackage extends HeaderClause {

    public ExportedPackage(String packageName, Attrs attribs) {
        super(packageName, attribs);
    }

    @Override
    protected boolean newlinesBetweenAttributes() {
        return false;
    }

    public void setVersionString(String version) {
        attribs.put(Constants.VERSION_ATTRIBUTE, version);
    }

    public String getVersionString() {
        return attribs.get(Constants.VERSION_ATTRIBUTE);
    }

    public boolean isProvided() {
        return Boolean.valueOf(attribs.get(aQute.lib.osgi.Constants.PROVIDE_DIRECTIVE));
    }

    public void setProvided(boolean provided) {
        if (provided)
            attribs.put(aQute.lib.osgi.Constants.PROVIDE_DIRECTIVE, Boolean.toString(true));
        else
            attribs.remove(aQute.lib.osgi.Constants.PROVIDE_DIRECTIVE);
    }

    @Override
    public ExportedPackage clone() {
        return new ExportedPackage(this.name, new Attrs(this.attribs));
    }
}
