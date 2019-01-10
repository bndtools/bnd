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

import org.eclipse.jface.fieldassist.IContentProposal;

class ErrorContentProposal implements IContentProposal {

    private final String message;

    public ErrorContentProposal(String message) {
        this.message = message;
    }

    @Override
    public String getContent() {
        return "";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getLabel() {
        return message;
    }

    @Override
    public int getCursorPosition() {
        return 0;
    }
}