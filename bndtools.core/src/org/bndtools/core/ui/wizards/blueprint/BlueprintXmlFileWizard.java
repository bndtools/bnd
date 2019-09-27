package org.bndtools.core.ui.wizards.blueprint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.framework.Bundle;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.osgi.Builder;
import aQute.libg.glob.Glob;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.editor.BndEditor;
import bndtools.editor.model.IDocumentWrapper;

public class BlueprintXmlFileWizard extends Wizard implements INewWizard {
	private static final String			OSGI_INF_BLUEPRINT_XML			= "OSGI-INF/blueprint/*.xml";
	private static final String			BLUEPRINT_TEMPLATES_EMPTY_XML	= "/blueprintTemplates/empty.xml";
	private static final String			BLUEPRINT_XML					= "blueprint.xml";

	private static final ILogger		logger							= Logger
		.getLogger(BlueprintXmlFileWizard.class);

	protected IStructuredSelection		selection;
	protected IWorkbench				workbench;

	protected WizardNewFileCreationPage	mainPage;
	protected WizardBndFileSelector		bndFileSelector;

	// Eclipse won't let us setContainerFullPath with a directory that doesn't
	// exist
	protected Stack<IFolder>			speculativelyCreatedFolders		= new Stack<>();

	@Override
	public void addPages() {
		mainPage = new WizardNewFileCreationPage("newFilePage", selection) {
			@Override
			protected InputStream getInitialContents() {
				return getTemplateContents();
			}
		};
		mainPage.setTitle("New Blueprint XML Descriptor");
		mainPage.setFileExtension("xml"); //$NON-NLS-1$
		mainPage.setAllowExistingResources(false);
		setupMainPageLocation();

		bndFileSelector = new WizardBndFileSelector();

		addPage(mainPage);
		addPage(bndFileSelector);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage) {
			bndFileSelector.updateControls(mainPage.getContainerFullPath());
		}
		return super.getNextPage(page);
	}

	private void setupMainPageLocation() {
		IFolder folder = null;
		Object selected = selection.getFirstElement();

		if (selected instanceof IFile) {
			selected = ((IFile) selected).getParent();
		}

		if (selected instanceof IProject) {
			IProject project = (IProject) selected;
			folder = project.getFolder("OSGI-INF/blueprint");
			if (!folder.exists()) {
				try {
					if (!folder.getParent()
						.exists()) {
						IFolder parent = (IFolder) folder.getParent();
						parent.create(false, true, null);
						speculativelyCreatedFolders.push(parent);
					}
					folder.create(false, true, null);
					speculativelyCreatedFolders.push(folder);
				} catch (CoreException e) {
					logger.logError(String.format("Unable to open to create folder: %s", folder.getFullPath()
						.toString()), e);
				}
			}
		} else if (selected instanceof IFolder) {
			folder = (IFolder) selected;
		}

		String name = BLUEPRINT_XML;
		if (folder != null) {
			mainPage.setContainerFullPath(folder.getFullPath());
			int i = 1;
			while (folder.getFile(name)
				.exists()) {
				name = "blueprint-" + i++ + ".xml";
			}
		}

		mainPage.setFileName(name);
	}

	@Override
	public boolean performCancel() {

		deleteSpeculativelyCreatedFolders();
		return super.performCancel();
	}

	private void deleteSpeculativelyCreatedFolders() {
		while (!speculativelyCreatedFolders.isEmpty()) {
			IFolder speculativelyCreatedFolder = speculativelyCreatedFolders.pop();
			try {
				speculativelyCreatedFolder.delete(false, null);
			} catch (CoreException e) {
				// Oh well - we're stuck with the folder...
				logger.logError(String.format("Unable to delete folder: %s", speculativelyCreatedFolder.getFullPath()
					.toString()), e);
			}
		}
	}

	@Override
	public boolean performFinish() {
		IFile file = mainPage.createNewFile();
		if (file == null) {
			deleteSpeculativelyCreatedFolders();
			return false;
		}

		if (!speculativelyCreatedFolders.isEmpty() && !speculativelyCreatedFolders.peek()
			.getFullPath()
			.isPrefixOf(file.getFullPath())) {
			deleteSpeculativelyCreatedFolders();
		}

		for (IFile bndFile : bndFileSelector.getSelectedBndFiles()) {
			try {
				updateBundleBlueprintAndIncludeResource(file, bndFile);
			} catch (Exception e) {
				ErrorDialog.openError(getShell(), "New Blueprint XML File", null,
					new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error updating Bnd file", e));
			}
		}

		// Open editor on new file.
		IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (PartInitException e) {
			ErrorDialog.openError(getShell(), "New Blueprint XML File", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening editor", e));
		}

		return true;
	}

	private void updateBundleBlueprintAndIncludeResource(IFile blueprintFile, IFile bndFile) throws Exception {

		BndEditModel editModel;
		IEditorPart editor = ResourceUtil.findEditor(workbench.getActiveWorkbenchWindow()
			.getActivePage(), bndFile);
		IDocument doc = null;
		if (editor instanceof BndEditor) {
			editModel = ((BndEditor) editor).getEditModel();
		} else {
			editModel = new BndEditModel(Central.getWorkspace());
			doc = FileUtils.readFully(bndFile);
			editModel.loadFrom(new IDocumentWrapper(doc));
		}

		String blueprintrelativePath = blueprintFile.getProjectRelativePath()
			.toString();

		updateBundleBlueprintIfNecessary(editModel, blueprintrelativePath);

		updateIncludeResourceIfNecessary(editModel, blueprintrelativePath, blueprintFile);

		if (editor == null) {
			editModel.saveChangesTo(new IDocumentWrapper(doc));
			FileUtils.writeFully(doc, bndFile, false);
		}
	}

	private void updateIncludeResourceIfNecessary(BndEditModel editModel, String blueprintrelativePath,
		IFile blueprintFile) throws Exception {
		try (Builder b = new Builder()) {
			b.setBase(blueprintFile.getProject()
				.getFullPath()
				.toFile());
			StringBuilder sb = new StringBuilder();
			List<String> includeResource = editModel.getIncludeResource();
			if (includeResource != null) {
				for (String s : includeResource) {
					sb.append(s)
						.append(',');
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
				}
				b.setIncludeResource(sb.toString());
			}
			if (!b.isInScope(Collections.singleton(blueprintFile.getFullPath()
				.toFile()))) {
				editModel.addIncludeResource(blueprintrelativePath);
			}
		}
	}

	private void updateBundleBlueprintIfNecessary(BndEditModel editModel, String blueprintrelativePath) {
		boolean alreadyMatched = false;
		List<HeaderClause> bundleBlueprint = editModel.getBundleBlueprint();
		if (bundleBlueprint != null) {
			for (HeaderClause hc : bundleBlueprint) {
				String clause = hc.getName();
				if (clause.length() == 0) {
					clause = OSGI_INF_BLUEPRINT_XML;
				} else if (clause.endsWith("/")) {
					clause += "*.xml";
				}
				// Match either absolute or Glob
				if ((!clause.contains("*") && clause.equals(blueprintrelativePath)) || (Glob.toPattern(clause)
					.matcher(blueprintrelativePath)
					.matches())) {
					alreadyMatched = true;
					break;
				}
			}
		}
		if (!alreadyMatched) {
			if ((Glob.toPattern(OSGI_INF_BLUEPRINT_XML)
				.matcher(blueprintrelativePath)
				.matches()))
				editModel.addBundleBlueprint(OSGI_INF_BLUEPRINT_XML);
			else
				editModel.addBundleBlueprint(blueprintrelativePath);
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	private InputStream getTemplateContents() throws IllegalArgumentException {

		Bundle bundle = Plugin.getDefault()
			.getBundle();

		try {
			URL entry = bundle.getEntry(BLUEPRINT_TEMPLATES_EMPTY_XML);
			return entry != null ? entry.openStream() : null;
		} catch (IOException e) {
			logger.logError(String.format("Unable to open template entry: %s in bundle %s",
				BLUEPRINT_TEMPLATES_EMPTY_XML, bundle.getSymbolicName()), e);
			return null;
		}
	}
}
