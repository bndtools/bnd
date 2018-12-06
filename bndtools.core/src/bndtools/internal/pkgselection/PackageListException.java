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
package bndtools.internal.pkgselection;

public class PackageListException extends Exception {

    private static final long serialVersionUID = 1L;

    public PackageListException(String message) {
        super(message);
    }

    public PackageListException(Throwable cause) {
        super(cause);
    }

    public PackageListException(String message, Throwable cause) {
        super(message, cause);
    }

}
