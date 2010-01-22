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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import aQute.lib.osgi.Constants;

public class ExportPackage extends HeaderClause {

	public ExportPackage(String name, Map<String, String> attribs) {
		super(name, attribs);
	}
	
	public List<String> getUses() {
		String usesStr = attribs.get(Constants.USES_DIRECTIVE);
		if(usesStr == null)
			return null;
		
		List<String> result = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(usesStr,",");
		while(tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken().trim());
		}
		return result;
	}

}
