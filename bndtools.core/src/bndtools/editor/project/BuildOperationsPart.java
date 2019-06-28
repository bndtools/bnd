package bndtools.editor.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class BuildOperationsPart extends SectionPart {

	private final Image	refreshImg	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_refresh.png")
		.createImage();
	private final Image	cleanImg	= AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/clear.gif")
		.createImage();

	/**
	 * Create the SectionPart.
	 *
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public BuildOperationsPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createClient(getSection(), toolkit);
	}

	/**
	 * Fill the section.
	 */
	private void createClient(Section section, FormToolkit toolkit) {
		section.setText("Build Operations");
		Composite container = toolkit.createComposite(section);

		section.setClient(container);
		container.setLayout(new GridLayout(1, false));

		ImageHyperlink lnkRebuildProject = toolkit.createImageHyperlink(container, SWT.NONE);
		lnkRebuildProject.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IResource resource = findEditorResource();
				if (resource != null) {
					RebuildJob job = new RebuildJob(resource.getProject(), IncrementalProjectBuilder.FULL_BUILD);
					job.setUser(true);
					job.schedule();
				}
			}
		});
		toolkit.paintBordersFor(lnkRebuildProject);
		lnkRebuildProject.setText("Rebuild Project");
		lnkRebuildProject.setImage(refreshImg);

		ImageHyperlink lnkCleanProject = toolkit.createImageHyperlink(container, SWT.NONE);
		toolkit.paintBordersFor(lnkCleanProject);
		lnkCleanProject.setText("Clean Project");
		lnkCleanProject.setImage(cleanImg);

		lnkCleanProject.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IResource resource = findEditorResource();
				if (resource != null) {
					RebuildJob job = new RebuildJob(resource.getProject(), IncrementalProjectBuilder.CLEAN_BUILD);
					job.setUser(true);
					job.schedule();
				}
			}

		});
	}

	private IResource findEditorResource() {
		IFormPage formPage = (IFormPage) getManagedForm().getContainer();
		IResource resource = ResourceUtil.getResource(formPage.getEditorInput());
		return resource;
	}

	private static class RebuildJob extends WorkspaceJob {

		private final IProject	project;
		private final int		buildType;

		public RebuildJob(IProject project, int buildType) {
			super("Rebuild");
			this.project = project;
			this.buildType = buildType;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			project.build(buildType, monitor);
			return Status.OK_STATUS;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		refreshImg.dispose();
		cleanImg.dispose();
	}

}
