package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.AnalyseToolbarAction;
import name.neilbartlett.eclipse.bndtools.utils.EditorUtils;
import name.neilbartlett.eclipse.bndtools.views.impexp.AnalyseImportsJob;
import name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;

public class ImportPatternsBlock extends MasterDetailsBlock {

	private ImportPatternsListPart listPart;

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		
		Composite container = toolkit.createComposite(parent);
		
		listPart = new ImportPatternsListPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(listPart);
		
		VersionPolicyPart versionPolicyPart = new VersionPolicyPart(container, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		versionPolicyPart.getSection().setExpanded(false);
		managedForm.addPart(versionPolicyPart);

		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(1, false));
		listPart.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		versionPolicyPart.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	@Override
	protected void createToolBarActions(final IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		
		AnalyseToolbarAction analyseAction = new AnalyseToolbarAction((IFormPage) managedForm.getContainer());
		analyseAction.setToolTipText("Analyse Imports/Exports");
		
		form.getToolBarManager().add(analyseAction);
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		ImportPatternsDetailsPage page = new ImportPatternsDetailsPage(listPart);
		detailsPart.registerPage(HeaderClause.class, page);
	}
}
