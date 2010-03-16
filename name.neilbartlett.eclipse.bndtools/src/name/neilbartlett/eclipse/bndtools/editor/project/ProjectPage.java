package name.neilbartlett.eclipse.bndtools.editor.project;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.utils.MessageHyperlinkAdapter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class ProjectPage extends FormPage {
	
	private BndEditModel model;

	public ProjectPage(FormEditor editor, BndEditModel model, String id, String title) {
		super(editor, id, title);
		this.model = model;
	}
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(model);

		FormToolkit tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("Project");
		tk.decorateFormHeading(form.getForm());
		form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter());

		// Create Controls
		Composite body = form.getBody();

		BuildPathPart buildPathPart = new BuildPathPart(body, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(buildPathPart);

		RunBundlesPart runBundlesPart = new RunBundlesPart(body, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		managedForm.addPart(runBundlesPart);

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(1, false);
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.TOP, false, false);
		buildPathPart.getSection().setLayoutData(gd);
		gd = new GridData(SWT.FILL, SWT.TOP, false, false);
		runBundlesPart.getSection().setLayoutData(gd);
	};
}