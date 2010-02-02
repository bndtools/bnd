package name.neilbartlett.eclipse.bndtools.project.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class BndProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Exported Bundle Directories:");
		
		Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}

}
