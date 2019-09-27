package bndtools;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.central.Central;

public class OpenExtConfigsContributionItem extends CompoundContributionItem {

	private static final ILogger				logger		= Logger.getLogger(OpenExtConfigsContributionItem.class);

	private static final IContributionItem[]	EMPTY		= new IContributionItem[0];
	private static final ImageDescriptor		extFileImg	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bullet_go.png");

	@Override
	protected IContributionItem[] getContributionItems() {
		try {
			IFile buildFile = Central.getWorkspaceBuildFile();
			if (buildFile == null)
				return EMPTY;

			IContainer cnfDir = buildFile.getParent();
			IFolder extDir = cnfDir.getFolder(new Path("ext"));
			if (extDir == null || !extDir.exists())
				return EMPTY;

			IResource[] extFiles = extDir.members();
			List<IContributionItem> result = new ArrayList<>(extFiles.length);

			for (final IResource extFile : extFiles) {
				if (extFile.getType() == IResource.FILE && "bnd".equalsIgnoreCase(extFile.getFileExtension())) {
					Action action = new Action() {
						@Override
						public void run() {
							try {
								FileEditorInput input = new FileEditorInput((IFile) extFile);

								IWorkbenchPage page = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow()
									.getActivePage();
								page.openEditor(input, "bndtools.bndWorkspaceConfigEditor", true);
							} catch (PartInitException e) {
								ErrorDialog.openError(PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow()
									.getShell(), "Error", "Unable to open editor", e.getStatus());
							}
						}
					};
					action.setText("Open " + extFile.getProjectRelativePath());
					action.setImageDescriptor(extFileImg);
					result.add(new ActionContributionItem(action));
				}
			}

			return result.toArray(new IContributionItem[0]);
		} catch (Exception e) {
			logger.logError("Unable to find default config files", e);
			return EMPTY;
		}

	}

}
