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

public class UIConstants {

    private static final char[] AUTO_ACTIVATION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890._".toCharArray(); //$NON-NLS-1$

    public static final char[] autoActivationCharacters() {
        char[] result = new char[AUTO_ACTIVATION_CHARS.length];
        System.arraycopy(AUTO_ACTIVATION_CHARS, 0, result, 0, AUTO_ACTIVATION_CHARS.length);
        return result;
    }

}
