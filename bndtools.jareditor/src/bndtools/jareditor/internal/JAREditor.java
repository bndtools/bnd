package bndtools.jareditor.internal;

import java.net.URI;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.exceptions.ConsumerWithException;
import aQute.bnd.exceptions.FunctionWithException;
import aQute.lib.strings.Strings;

public class JAREditor extends FormEditor implements IResourceChangeListener {
	private static final ILogger	logger		= Logger.getLogger(JAREditor.class);
	private JARTreePage				treePage	= new JARTreePage(this, "treePage", "Tree");
	private JARPrintPage			printPage	= new JARPrintPage(this, "printPage", "Print");
	private URI						uri;

	@Override
	protected void addPages() {
		try {
			addPage(treePage);
			addPage(printPage);
		} catch (PartInitException e) {
			JAREditor.error("Could not initialize the JAREditor: " + e);
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
		super.init(site, input);
		IResource resource = ResourceUtil.getResource(input);
		if (resource != null) {
			resource.getWorkspace()
				.addResourceChangeListener(this);
		}
	}

	@Override
	protected void setInput(final IEditorInput input) {
		super.setInput(input);

		String name = "unknown";
		if (input instanceof IFileEditorInput) {
			name = ((IFileEditorInput) input).getFile()
				.getName();
		} else if (input instanceof IURIEditorInput) {
			name = ((IURIEditorInput) input).getName();
		}
		setPartName(name);
		setInput(retrieveFileURI(input));
	}

	@Override
	public void dispose() {
		IResource resource = ResourceUtil.getResource(getEditorInput());

		super.dispose();

		if (resource != null) {
			resource.getWorkspace()
				.removeResourceChangeListener(this);
		}
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		IResource myResource = ResourceUtil.getResource(getEditorInput());

		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return;
		}

		IPath fullPath = myResource.getFullPath();
		delta = delta.findMember(fullPath);
		if (delta == null) {
			return;
		}

		if (delta.getKind() == IResourceDelta.REMOVED) {
			close(false);
		} else if (delta.getKind() == IResourceDelta.CHANGED) {
			Display.getDefault()
				.asyncExec(this::update);
		}
	}

	private void setInput(URI uri) {
		this.uri = uri;
		update();
	}

	private void update() {
		treePage.setInput(uri);
		printPage.setInput(uri);
	}

	static <T> void background(String message, FunctionWithException<IProgressMonitor, ? extends T> background,
		ConsumerWithException<? super T> onDisplayThread) {

		Job job = new Job(message) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					T result = background.apply(monitor);
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					Display.getDefault()
						.asyncExec(() -> {
							try {
								onDisplayThread.accept(result);
							} catch (Exception e) {
								logger.logError("JAREditor display thread exception! " + message, e);
							}
						});
					return Status.OK_STATUS;
				} catch (Exception e) {
					JAREditor.error("Failed " + message + " : " + e.getMessage());
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e);
				}
			}
		};
		job.schedule();
	}

	static void error(Throwable e, String format, Object... args) {
		String message = Strings.format(format, args);
		Status status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e);
		Display.getDefault()
			.asyncExec(() -> {
				ErrorDialog.openError(null, "Error", message, status);
			});
	}

	static void error(String message) {
		error(null, "%s", message);
	}

	private URI retrieveFileURI(final IEditorInput input) {
		URI uri = null;
		if (input instanceof IFileEditorInput) {
			uri = ((IFileEditorInput) input).getFile()
				.getLocationURI();
		} else if (input instanceof IURIEditorInput) {
			uri = ((IURIEditorInput) input).getURI();
		}

		return uri;
	}

	static boolean isDisplayThread() {
		return Display.getDefault()
			.getThread() == Thread.currentThread();
	}
}
