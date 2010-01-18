package name.neilbartlett.eclipse.bndtools.editor.exports;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExportPatternsBlock extends MasterDetailsBlock {

	private ExportPatternsListPart listPart;

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		
		Composite container = toolkit.createComposite(parent);
		
		listPart = new ExportPatternsListPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(listPart);
		
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(1, false));
		listPart.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		PkgPatternsDetailsPage page = new PkgPatternsDetailsPage(listPart, "Export Pattern Details");
		detailsPart.registerPage(HeaderClause.class, page);
	}
}
