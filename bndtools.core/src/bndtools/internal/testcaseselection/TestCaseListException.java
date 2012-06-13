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
package bndtools.internal.testcaseselection;

public class TestCaseListException extends Exception {

    private static final long serialVersionUID = 1L;

    public TestCaseListException(String message) {
        super(message);
    }

    public TestCaseListException(Throwable cause) {
        super(cause);
    }

    public TestCaseListException(String message, Throwable cause) {
        super(message, cause);
    }

}
