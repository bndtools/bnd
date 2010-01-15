package name.neilbartlett.eclipse.bndtools.editor;

import java.lang.reflect.InvocationTargetException;

import name.neilbartlett.eclipse.bndtools.MakeBundleWithRefreshAction;
import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BuildFormPart extends SectionPart {

	private MakeBundleWithRefreshAction buildBundleAction;
	private Image jarImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_obj.gif").createImage();

	public BuildFormPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
	
		buildBundleAction = new MakeBundleWithRefreshAction();
		createSection(getSection(), toolkit);
	}

	void createSection(final Section section, FormToolkit toolkit) {
		section.setText("Build");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		ImageHyperlink lnkBuild = toolkit.createImageHyperlink(composite, SWT.LEFT);
		lnkBuild.setText("Build Bundle");
		lnkBuild.setImage(jarImg);

		lnkBuild.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IFormPage page = (IFormPage) getManagedForm().getContainer();
				final FormEditor editor = page.getEditor();
				if(editor.isDirty()) {
					if(MessageDialog.openConfirm(section.getShell(), "Build Bundle", "The editor content must be saved before building.")) {
						IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								editor.doSave(monitor);
							}
						};
						IWorkbenchWindow window = getEditor().getSite().getWorkbenchWindow();
						try {
							window.run(false, false, saveRunnable);
						} catch (InvocationTargetException e1) {
						} catch (InterruptedException e1) {
							Thread.currentThread().interrupt();
						}
					}
				}
				if(editor.isDirty()) {
					MessageDialog.openError(section.getShell(), "Build Bundle", "Bundle not built due to error during save.");
					return;
				}
				buildBundleAction.run(null);
			}
		});
		
		composite.setLayout(new GridLayout(1, false));
	}
	BndEditor getEditor() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		return (BndEditor) page.getEditor();
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		IFormPage page = (IFormPage) form.getContainer();
		buildBundleAction.setActiveEditor(null, page.getEditor());
	}
	@Override
	public void dispose() {
		super.dispose();
		jarImg.dispose();
	}
}
