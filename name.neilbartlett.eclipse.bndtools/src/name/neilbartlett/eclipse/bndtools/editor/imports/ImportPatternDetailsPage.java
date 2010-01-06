package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class ImportPatternDetailsPage extends AbstractFormPart implements
		IDetailsPage, PropertyChangeListener {

	private BndEditModel model;
	private ImportPattern pattern;
	
	private Text txtPattern;
	private Button btnOptional;

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		FieldDecoration contentProposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Import Pattern Detail");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Pattern:");
		txtPattern = toolkit.createText(composite, "");
		ControlDecoration decPattern = new ControlDecoration(txtPattern, SWT.LEFT | SWT.TOP, composite);
		decPattern.setImage(contentProposalDecoration.getImage());
		decPattern.setDescriptionText("Content assist available"); // TODO: keystrokes
		decPattern.setShowHover(true);
		decPattern.setShowOnlyOnFocus(true);
		
		btnOptional = toolkit.createButton(composite, "Optional resolution", SWT.CHECK);
		
		// Layout
		parent.setLayout(new GridLayout(1, false));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(2, false));

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 5;
		txtPattern.setLayoutData(gd);
		btnOptional.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if(!selection.isEmpty() && selection instanceof IStructuredSelection) {
			this.pattern = (ImportPattern) ((IStructuredSelection) selection).getFirstElement();
		} else {
			this.pattern = null;
		}
		updateFields();
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		updateFields();
	}

	void updateFields() {
		txtPattern.setText(pattern != null ? pattern.getPattern() : "");
		btnOptional.setSelection(pattern != null && pattern.isOptional());
	}
	
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		Object container = getManagedForm().getContainer();
		
		System.out.println(container);
	}
	
}
