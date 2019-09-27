package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class IncludedResourcesPart extends SectionPart implements PropertyChangeListener {

	private Table table;

	public IncludedResourcesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Import Patterns");
		section.setDescription("Resources matching the listed patterns will be included in the bundle.");

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);

		// Layout
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		GridData gd;
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		gd.widthHint = 300;
		gd.heightHint = 100;
		table.setLayoutData(gd);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {}
}
