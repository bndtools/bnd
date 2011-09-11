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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;

public class ImportPattern extends VersionedClause implements Cloneable {

	public ImportPattern(String pattern, Map<String, String> attributes) {
		super(pattern, attributes);
	}
	public boolean isOptional() {
		String resolution = attribs.get(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}

    public void setOptional(boolean optional) {
        if (optional)
            attribs.put(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
        else
            attribs.remove(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
    }

	@Override
	public ImportPattern clone() {
		return new ImportPattern(this.name, new HashMap<String, String>(this.attribs));
	}
}
