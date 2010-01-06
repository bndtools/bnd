package name.neilbartlett.eclipse.bndtools.editor.components;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ComponentDetailsPage extends AbstractFormPart implements IDetailsPage {

	private Text txtPath;

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		FieldDecoration contentProposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Component Details");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Path:");
		txtPath = toolkit.createText(composite, "");
		ControlDecoration decPattern = new ControlDecoration(txtPath, SWT.LEFT | SWT.TOP, composite);
		decPattern.setImage(contentProposalDecoration.getImage());
		decPattern.setDescriptionText("Content assist available"); // TODO: keystrokes
		decPattern.setShowHover(true);
		decPattern.setShowOnlyOnFocus(true);
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
	}

}
