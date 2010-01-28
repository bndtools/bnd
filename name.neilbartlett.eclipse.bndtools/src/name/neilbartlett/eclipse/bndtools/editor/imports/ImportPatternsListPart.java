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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsListPart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.lib.osgi.Constants;

public class ImportPatternsListPart extends PkgPatternsListPart {

	private class FixMissingStarsAction extends Action {
		public FixMissingStarsAction(String message) {
			super(message);
		}
		public void run() {
			// Remove existing "*" patterns that are not in the last place
			List<HeaderClause> toRemove = new LinkedList<HeaderClause>();
			for (Iterator<HeaderClause> iter = getClauses().iterator(); iter.hasNext(); ) {
				HeaderClause clause = iter.next();
				if(clause.getName().equals("*") && iter.hasNext()) {
					toRemove.add(clause);
				}
			}
			if(!toRemove.isEmpty()) {
				doRemoveClauses(toRemove);
			}
			
			// Add a "*" at the end, if not already present
			List<HeaderClause> clauses = getClauses();
			if(clauses.size() != 0 && !clauses.get(clauses.size() - 1).getName().equals("*")) {
				HeaderClause starPattern = new HeaderClause("*", new HashMap<String, String>());
				ImportPatternsListPart.super.doAddClauses(Arrays.asList(starPattern), -1, false);
			}
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
		String actionMessage = null;
		List<HeaderClause> clauses = getClauses();
		if(!clauses.isEmpty()) {
			for(Iterator<HeaderClause> iter = clauses.iterator(); iter.hasNext();) {
				HeaderClause clause = iter.next();
				if(clause.getName().equals("*") && iter.hasNext()) {
					noStarWarning = "The catch-all pattern \"*\" should be in the last position.";
					actionMessage = "Move \"*\" pattern to the last position.";
					break;
				}
			}
			
			if(noStarWarning == null) {
				HeaderClause last = clauses.get(clauses.size() - 1);
				if(!last.getName().equals("*")) {
					noStarWarning = "The catch-all pattern \"*\" should be present and in the last position.";
					actionMessage = "Add missing \"*\" pattern.";
				}
			}
		}
		if(noStarWarning != null) {
			msgs.addMessage("_warning_no_star", noStarWarning, new FixMissingStarsAction(actionMessage) , IMessageProvider.WARNING);
		} else {
			msgs.removeMessage("_warning_no_star");
		}
	}
}
