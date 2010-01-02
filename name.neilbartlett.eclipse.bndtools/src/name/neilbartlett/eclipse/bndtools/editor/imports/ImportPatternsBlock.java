package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ImportPatternsBlock extends MasterDetailsBlock {

	private ImportPatternsPart importPatternsPart;
	private ImportPatternDetailsPage patternDetailsPage;

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		
		Composite container = toolkit.createComposite(parent);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(1, false));
		
		importPatternsPart = new ImportPatternsPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(importPatternsPart);
		importPatternsPart.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		VersionPolicyPart versionPolicyPart = new VersionPolicyPart(container, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		versionPolicyPart.getSection().setExpanded(false);
		managedForm.addPart(versionPolicyPart);

	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		detailsPart.setPageProvider(new IDetailsPageProvider() {
			public Object getPageKey(Object object) {
				if(object instanceof ImportPattern)
					return ImportPattern.class;
				
				return object.getClass();
			}
			public IDetailsPage getPage(Object key) {
				return null;
			}
		});
		
		patternDetailsPage = new ImportPatternDetailsPage();
		detailsPart.registerPage(ImportPattern.class, patternDetailsPage);
	}
}
