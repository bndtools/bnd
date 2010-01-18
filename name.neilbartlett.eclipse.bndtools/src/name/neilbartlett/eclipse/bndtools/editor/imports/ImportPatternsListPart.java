package name.neilbartlett.eclipse.bndtools.editor.imports;

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
			doAddClause(starPattern);
		}
	};
	public ImportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style, Constants.IMPORT_PACKAGE);
	}
	protected void doAdd() {
		boolean appendStar = getClauses().isEmpty();
		
		super.doAdd();
		
		if(appendStar) {
			HeaderClause starPattern = new HeaderClause("*", new HashMap<String, String>());
			doAddClause(starPattern);
		}
	};
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
