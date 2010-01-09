package name.neilbartlett.eclipse.bndtools.editor.components;

import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ComponentDetailsPage extends AbstractFormPart implements IDetailsPage {

	private ServiceComponent selected;
	
	private Text txtPath;
	private Text txtName;
	private Button btnEnabled;
	private Text txtFactoryId;

	private Text txtActivate;
	private Text txtDeactivate;
	private Text txtModified;
	private Button btnImmediate;
	private Button btnSvcFactory;

	private Table tableProvide;
	private TableViewer viewerProvide;
	private Button btnAddProvide;
	private Button btnRemoveProvide;
	
	private Button btnConfigPolicyOptional;
	private Button btnConfigPolicyRequire;
	private Button btnConfigPolicyIgnore;

	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		// Create Controls
		Section mainSection = toolkit.createSection(parent, Section.TITLE_BAR);
		mainSection.setText("Component Details");
		fillMainSection(toolkit, mainSection);

		Section lifecycleSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		lifecycleSection.setText("Lifecycle");
		fillLifecycleSection(toolkit, lifecycleSection);
		
		Section provideSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		provideSection.setText("Provided Services");
		fillProvideSection(toolkit, provideSection);
		
		Section configPolicySection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		configPolicySection.setText("Configuration Policy");
		fillConfigPolicySection(toolkit, configPolicySection);
		
		// Layout
		parent.setLayout(new GridLayout(1, false));
		mainSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		lifecycleSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		provideSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		configPolicySection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	void fillMainSection(FormToolkit toolkit, Section section) {
		FieldDecoration contentProposalDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Path:");
		txtPath = toolkit.createText(composite, "");
		decor = new ControlDecoration(txtPath, SWT.LEFT | SWT.TOP, composite);
		decor.setImage(contentProposalDecoration.getImage());
		decor.setDescriptionText("Content assist available"); // TODO: keystrokes
		decor.setShowHover(true);
		decor.setShowOnlyOnFocus(true);
		
		toolkit.createLabel(composite, "Name:");
		txtName = toolkit.createText(composite, "");
		toolkit.createLabel(composite, ""); // Spacer
		btnEnabled = toolkit.createButton(composite, "Enabled", SWT.CHECK);
		
		toolkit.createLabel(composite, "Factory ID:");
		txtFactoryId = toolkit.createText(composite, "");
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		txtPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	void fillProvideSection(FormToolkit toolkit, Section section) {
		// Create controls
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		tableProvide = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		viewerProvide = new TableViewer(tableProvide);
		
		btnAddProvide = toolkit.createButton(composite, "Add", SWT.PUSH);
		btnRemoveProvide = toolkit.createButton(composite, "Remove", SWT.PUSH);
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		GridData gd;
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
		gd.heightHint = 75;
		tableProvide.setLayoutData(gd);
		btnAddProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemoveProvide.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void fillLifecycleSection(FormToolkit toolkit, Section section) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		// Create controls
		toolkit.createLabel(composite, "Activate method:");
		txtActivate = toolkit.createText(composite, "");
		toolkit.createLabel(composite, "Deactivate method:");
		txtDeactivate = toolkit.createText(composite, "");
		toolkit.createLabel(composite, "Modified method:");
		txtModified = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, ""); // Spacer
		btnImmediate = toolkit.createButton(composite, "Immediate", SWT.CHECK);
		decor = new ControlDecoration(btnImmediate, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated immediately,\n" +
		                         "even when it provides a service and no consumers\n" +
		                         "of the service exist.");
		decor.setShowHover(true);
		
		toolkit.createLabel(composite, ""); // Spacer
		btnSvcFactory = toolkit.createButton(composite, "Service Factory", SWT.CHECK);
		decor = new ControlDecoration(btnSvcFactory, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("An instance of the component will be created\nfor each service consumer.");
		decor.setShowHover(true);
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		txtActivate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtDeactivate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtModified.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	void fillConfigPolicySection(FormToolkit toolkit, Section section) {
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		ControlDecoration decor;
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		btnConfigPolicyOptional = toolkit.createButton(composite, "Optional", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyOptional, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated whether or not\nmatching configuration data is available.");
		decor.setShowHover(true);
		btnConfigPolicyRequire = toolkit.createButton(composite, "Require", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyRequire, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will be activated ONLY if matching\nconfiguration data is available.");
		decor.setShowHover(true);
		btnConfigPolicyIgnore = toolkit.createButton(composite, "Ignore", SWT.RADIO);
		decor = new ControlDecoration(btnConfigPolicyIgnore, SWT.RIGHT, composite);
		decor.setImage(infoDecoration.getImage());
		decor.setDescriptionText("The component will not receive configuration\ndata from Configuration Admin.");
		decor.setShowHover(true);
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		this.selected = (ServiceComponent) ((IStructuredSelection) selection).getFirstElement();
		
		showSelection();
	}
	private void showSelection() {
		txtPath.setText(selected != null ? selected.getPattern() : "");
	}
}
