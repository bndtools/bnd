package name.neilbartlett.eclipse.bndtools.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PackagesPart extends SectionPart {

	private Table table;

	public PackagesPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Included Packages");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI);
		Button btnAdd = toolkit.createButton(composite, "Add", SWT.PUSH);
		
		// Layout
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		GridData gd;
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		table.setLayoutData(gd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		btnAdd.setLayoutData(gd);
	}

}
