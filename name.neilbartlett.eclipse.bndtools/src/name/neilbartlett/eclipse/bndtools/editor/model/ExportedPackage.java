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
package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.Map;

import org.osgi.framework.Constants;

public class ExportedPackage extends HeaderClause {
	public ExportedPackage(String packageName, Map<String, String> attribs) {
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
	
	@Override
	public ExportedPackage clone() {
		return new ExportedPackage(this.name, this.attribs);
	}
}
