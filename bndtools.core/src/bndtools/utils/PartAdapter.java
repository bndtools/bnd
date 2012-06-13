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
package bndtools.utils;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

public abstract class PartAdapter implements IPartListener {

    public void partActivated(IWorkbenchPart part) {}

    public void partBroughtToTop(IWorkbenchPart part) {}

    public void partClosed(IWorkbenchPart part) {}

    public void partDeactivated(IWorkbenchPart part) {}

    public void partOpened(IWorkbenchPart part) {}
}
