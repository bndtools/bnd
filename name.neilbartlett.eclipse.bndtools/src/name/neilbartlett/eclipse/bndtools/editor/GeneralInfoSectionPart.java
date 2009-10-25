package name.neilbartlett.eclipse.bndtools.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class GeneralInfoSectionPart extends SectionPart {

	public GeneralInfoSectionPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		createSection(getSection(), toolkit);
	}

	// Called during construction => private
	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("General Information");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, Constants.BUNDLE_SYMBOLICNAME);
		Text txtBSN = toolkit.createText(composite, "");
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		txtBSN.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

}
