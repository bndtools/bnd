package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

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
	protected void createToolBarActions(IManagedForm managedForm) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		ImportPatternsDetailsPage page = new ImportPatternsDetailsPage(listPart);
		detailsPart.registerPage(HeaderClause.class, page);
	}
}
