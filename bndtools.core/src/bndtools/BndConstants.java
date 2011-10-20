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
package bndtools;

import aQute.lib.osgi.Constants;

public interface BndConstants extends Constants {

    public static final String OUTPUT = "-output";
    public static final String RUNFRAMEWORK = "-runfw";
    public static final String REQUIRE_OBR = "-requireobr";

    /**
     * @deprecated Use {@link Constants#RUNVM}.
     */
    @Deprecated
    public static final String RUNVMARGS = "-vmargs";

    /**
     * @deprecated Use {@link Constants#TESTCASES}.
     */
    @Deprecated
    public static final String TESTSUITES = "Test-Suites";

    public static final String RUNREQUIRE = "-runrequire";
    public static final String RUNEE = "-runee";
}
