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
package bndtools.jareditor.internal.utils;

import org.eclipse.swt.widgets.Display;

public class SWTConcurrencyUtil {

    public static void execForDisplay(Display display, Runnable op) {
        if (display != null && !display.isDisposed()) {
            if (display.getThread() == Thread.currentThread()) {
                op.run();
            } else {
                display.asyncExec(op);
            }
        }
    }

}
