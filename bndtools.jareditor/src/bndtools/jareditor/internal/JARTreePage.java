package bndtools.jareditor.internal;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private IFolder				folder;
	private boolean				closed;
	private final Updater		updater	= new Updater();

	class Updater {
		final AtomicBoolean	loading	= new AtomicBoolean(false);
		int					epoch	= 1000;
		boolean				active	= false;

		void activate(boolean active) {
			assert JAREditor.isDisplayThread();
			boolean prev = this.active;
			this.active = active;

			if (active && !prev)
				refresh();
		}

		void refresh() {
			if (uri == null)
				return;
			epoch++;

			if (loading.getAndSet(true) == false) {
				refresh0();
			}
		}

		void refresh0() {
			assert JAREditor.isDisplayThread();

			URI loadingUri = uri;
			int currentEpoch = this.epoch;

			JAREditor.background("Reading jar/zip file", monitor -> {
				return getFolder(loadingUri, monitor);
			}, folder -> {

				assert JAREditor.isDisplayThread();
				if (!active || closed) {
					System.out.println("closed");
					TemporaryFile.dispose(folder);
					return;
				}

				assert tree != null;

				if (epoch != currentEpoch) {
					TemporaryFile.dispose(folder);
					refresh0();
				} else {
					loading.set(false);
					setFolder(folder);
					tree.setFormInput(folder);
				}
			});
		}

	}

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
		updater.activate(active);
	}

	@Override
	public void dispose() {
		this.closed = true;
		updater.activate(false);
		titleImg.dispose();
		setFolder(null);
		super.dispose();
	}

	private void setFolder(IFolder folder) {
		TemporaryFile.dispose(this.folder);
		this.folder = folder;
	}

	void setInput(URI uri) {
		assert JAREditor.isDisplayThread();
		this.uri = uri;
		updater.refresh();
	}

	/*
	 * Purely functional to read the JAR file.
	 * @param input the input URL
	 * @param monitor the monitor to use
	 * @return a folder
	 */
	private static IFolder getFolder(URI input, IProgressMonitor monitor) throws CoreException {
		URI full = JarFileSystem.jarf(input, "/")
			.orElseThrow(IllegalArgumentException::new);
		assert full.getPath() != null;
		IFileStore store = EFS.getStore(full);

		return TemporaryFile.tempFolder(store.toURI(), input.getPath(), monitor)
			.orElseThrow(IllegalArgumentException::new);
	}

}
