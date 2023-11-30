package bndtools.jareditor.internal;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JARTreePage extends FormPage {

	private Image				titleImg;
	private JARTreePart			tree;
	private JARTreeEntryPart	entry;
	private URI					uri;
	final AtomicInteger			loading	= new AtomicInteger();
	private IFolder				folder;
	private boolean				closed;

	public JARTreePage(JAREditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		managedForm.setInput(getEditorInput());

		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		form.setText("JAR File Viewer");
		toolkit.decorateFormHeading(form.getForm());

		titleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_obj.gif")
			.createImage(form.getDisplay());
		form.setImage(titleImg);

		// CREATE CONTROLS
		Composite body = form.getBody();

		SashForm sashForm = new SashForm(body, SWT.HORIZONTAL);

		Composite treePanel = toolkit.createComposite(sashForm);
		tree = new JARTreePart(treePanel, managedForm);
		managedForm.addPart(tree);

		Composite detailsPanel = toolkit.createComposite(sashForm);
		entry = new JARTreeEntryPart(getEditor(), detailsPanel, toolkit);
		managedForm.addPart(entry);

		GridLayout layout;
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		body.setLayout(layout);

		sashForm.setWeights(new int[] {
			1, 3
		});
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public void setActive(boolean active) {

		assert JAREditor.isDisplayThread();

		super.setActive(active);
		if (active) {
			update();
		}
	}

	@Override
	public void dispose() {
		this.closed = true;
		titleImg.dispose();
		setFolder(null);
		super.dispose();
	}

	private void update() {

		assert JAREditor.isDisplayThread();

		if (tree == null || !isActive())
			return;

		if (loading.getAndIncrement() > 1)
			return;

		JAREditor.background("Reading zip file", monitor -> {
			IFolder folder;
			do {
				folder = getFolder(uri, monitor);
				loading.getAndDecrement();
			} while (loading.getAndSet(0) > 0);
			return folder;
		}, folder -> {
			if (closed)
				return;
			setFolder(folder);
			tree.setFormInput(folder);
		});
	}

	private void setFolder(IFolder folder) {
		TemporaryFile.dispose(this.folder);
		this.folder = folder;
	}

	void setInput(URI uri) {

		assert JAREditor.isDisplayThread();

		if (this.uri != null && this.uri.equals(uri))
			return;

		this.uri = uri;
		update();
	}

	private IFolder getFolder(URI input, IProgressMonitor monitor) throws CoreException {

		URI full = JarFileSystem.jarf(input, "/")
			.orElseThrow(IllegalArgumentException::new);
		assert full.getPath() != null;
		IFileStore store = EFS.getStore(full);

		return TemporaryFile.tempFolder(store.toURI(), input.getPath(), monitor)
			.orElseThrow(IllegalArgumentException::new);
	}

}
