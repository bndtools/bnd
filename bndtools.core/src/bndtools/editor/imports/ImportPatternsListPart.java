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
package bndtools.editor.imports;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;

import aQute.lib.osgi.Constants;
import aQute.libg.header.Attrs;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.model.clauses.ImportPattern;

public class ImportPatternsListPart extends PkgPatternsListPart<ImportPattern> {

	private class FixMissingStarsAction extends Action {
		public FixMissingStarsAction(String message) {
			super(message);
		}
		@Override
        public void run() {
			// Remove existing "*" patterns that are not in the last place
			List<ImportPattern> toRemove = new LinkedList<ImportPattern>();
			for (Iterator<ImportPattern> iter = getClauses().iterator(); iter.hasNext(); ) {
				ImportPattern pattern = iter.next();
				if(pattern.getName().equals("*") && iter.hasNext()) {
					toRemove.add(pattern);
				}
			}
			if(!toRemove.isEmpty()) {
				doRemoveClauses(toRemove);
			}

			// Add a "*" at the end, if not already present
			List<ImportPattern> patterns = getClauses();
			if(patterns.size() != 0 && !patterns.get(patterns.size() - 1).getName().equals("*")) {
				ImportPattern starPattern = new ImportPattern("*", new Attrs());
				ImportPatternsListPart.super.doAddClauses(Arrays.asList(starPattern), -1, false);
			}
		}
	};
	public ImportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.IMPORT_PACKAGE, "Customise Imports", new ImportPatternLabelProvider());
	}
	@Override
	protected void doAddClauses(Collection<? extends ImportPattern> clauses, int index, boolean select) {
		boolean appendStar = getClauses().isEmpty();

		super.doAddClauses(clauses, index, select);

		if(appendStar) {
			ImportPattern starPattern = new ImportPattern("*", new Attrs()); //$NON-NLS-1$
			super.doAddClauses(Arrays.asList(starPattern), -1, false);
		}
	}
	@Override
    public void validate() {
		IMessageManager msgs = getManagedForm().getMessageManager();
		msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);

		String noStarWarning = null;
		String actionMessage = null;
		List<ImportPattern> patterns = getClauses();
		if(!patterns.isEmpty()) {
			for(Iterator<ImportPattern> iter = patterns.iterator(); iter.hasNext();) {
				ImportPattern pattern = iter.next();
				if(pattern.getName().equals("*") && iter.hasNext()) {
					noStarWarning = "The catch-all pattern \"*\" should be in the last position.";
					actionMessage = "Move \"*\" pattern to the last position.";
					break;
				}
			}

			if(noStarWarning == null) {
				ImportPattern last = patterns.get(patterns.size() - 1);
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
	@Override
	protected ImportPattern newHeaderClause(String text) {
		return new ImportPattern(text, new Attrs());
	}
	@Override
	protected List<ImportPattern> loadFromModel(BndEditModel model) {
		return model.getImportPatterns();
	}
	@Override
	protected void saveToModel(BndEditModel model, List<? extends ImportPattern> clauses) {
		model.setImportPatterns(clauses);
	}
}
