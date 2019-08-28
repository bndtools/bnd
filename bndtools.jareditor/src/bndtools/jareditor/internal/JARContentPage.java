package bndtools.jareditor.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARContentPage extends FormPage {

	private Image				titleImg;
	private JARContentTreePart	contentTreePart;
	private JAREntryPart		entryPart;

	private String[]			selectedPath;

	public JARContentPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(getEditorInput());

		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("JAR File Viewer");
		toolkit.decorateFormHeading(form.getForm());

		titleImg = AbstractUIPlugin.imageDescriptorFromPlugin(PluginConstants.PLUGIN_ID, "/icons/jar_obj.gif")
			.createImage(form.getDisplay());
		form.setImage(titleImg);

		// CREATE CONTROLS
		Composite body = form.getBody();

		SashForm sashForm = new SashForm(body, SWT.HORIZONTAL);

		Composite treePanel = toolkit.createComposite(sashForm);
		contentTreePart = new JARContentTreePart(treePanel, managedForm);
		managedForm.addPart(contentTreePart);

		Composite detailsPanel = toolkit.createComposite(sashForm);
		entryPart = new JAREntryPart(getEditor(), detailsPanel, toolkit);
		managedForm.addPart(entryPart);

		if (selectedPath != null)
			contentTreePart.setSelectedPath(selectedPath);

		// LAYOUT
		GridLayout layout;
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		body.setLayout(layout);

		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// treeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));
		// detailsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));
	}

	@Override
	public void dispose() {
		super.dispose();
		titleImg.dispose();
	}

	void setSelectedPath(String[] path) {
		this.selectedPath = path;
		if (contentTreePart != null) {
			contentTreePart.setSelectedPath(path);
		}
	}

	String[] getSelectedPath() {
		if (contentTreePart != null) {
			return contentTreePart.getSelectedPath();
		}

		return null;
	}
}
