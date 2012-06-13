/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.utils;

import java.util.Iterator;
import java.util.List;

public class CircularDependencyException extends Exception {

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("rawtypes")
    private final List circle;

    public CircularDependencyException(@SuppressWarnings("rawtypes")
    List circle) {
        this.circle = circle;
    }

    @Override
    public String getLocalizedMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("Artifacts in cycle: ");
        for (Iterator< ? > iter = circle.iterator(); iter.hasNext();) {
            builder.append(iter.next().toString());
            if (iter.hasNext())
                builder.append(" -> ");
        }
        return builder.toString();
    }

}
