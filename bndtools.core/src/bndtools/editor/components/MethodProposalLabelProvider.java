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
package bndtools.editor.components;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class MethodProposalLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
	@Override
	public void update(ViewerCell cell) {
		StyledString styledString = getStyledString(cell.getElement());
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
	public String getText(Object element) {
		return getStyledString(element).getString();
	}
	private StyledString getStyledString(Object element) {
		MethodContentProposal proposal = (MethodContentProposal) element;
		
		IMethod method = proposal.getMethod();
		String methodName = method.getElementName();
		IType type = method.getDeclaringType();
		String typeName = type.getElementName();
		
		StyledString styledString = new StyledString(methodName);
		styledString.append(": " + typeName, StyledString.QUALIFIER_STYLER);
		
		return styledString;
	}
	public Image getImage(Object element) {
		return null;
	}
}
