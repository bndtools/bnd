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
package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsListPart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.lib.osgi.Constants;

public class ImportPatternsListPart extends PkgPatternsListPart {

	private final IAction fixMissingStarPatternAction = new Action("Append missing \"*\" pattern.") {
		public void run() {
			HeaderClause starPattern = new HeaderClause("*", new HashMap<String, String>());
			ImportPatternsListPart.super.doAddClauses(Arrays.asList(starPattern), -1, false);
		}
	};
	public ImportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.IMPORT_PACKAGE);
	}
	@Override
	protected void doAddClauses(Collection<? extends HeaderClause> clauses, int index, boolean select) {
		boolean appendStar = getClauses().isEmpty();
		
		super.doAddClauses(clauses, index, select);
		
		if(appendStar) {
			HeaderClause starPattern = new HeaderClause("*", new HashMap<String, String>()); //$NON-NLS-1$
			super.doAddClauses(Arrays.asList(starPattern), -1, false);
		}
	}
	public void validate() {
		IMessageManager msgs = getManagedForm().getMessageManager();
		msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);
		
		String noStarWarning = null;
		List<HeaderClause> clauses = getClauses();
		if(!clauses.isEmpty()) {
			HeaderClause last = clauses.get(clauses.size() - 1);
			if(!last.getName().equals("*"))
				noStarWarning = "The catch-all pattern \"*\" should be present and in the last position.";
		}
		if(noStarWarning != null) {
			msgs.addMessage("_warning_no_star", noStarWarning, fixMissingStarPatternAction, IMessageProvider.WARNING);
		} else {
			msgs.removeMessage("_warning_no_star");
		}
	}
}
