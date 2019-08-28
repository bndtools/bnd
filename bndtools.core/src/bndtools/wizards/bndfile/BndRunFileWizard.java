package bndtools.wizards.bndfile;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.core.ui.wizards.shared.BuiltInTemplate;
import org.bndtools.core.ui.wizards.shared.ISkippableWizardPage;
import org.bndtools.core.ui.wizards.shared.TemplateParamsWizardPage;
import org.bndtools.core.ui.wizards.shared.TemplateSelectionWizardPage;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.Template;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import bndtools.Plugin;

public class BndRunFileWizard extends Wizard implements INewWizard {

	private static final String			PROP_PROJECT_NAME		= "projectName";
	private static final String			PROP_FILE_BASE_NAME		= "fileBaseName";
	private static final String			PROP_FILE_NAME			= "fileName";
	private static final String[]		PROPS					= new String[] {
		PROP_FILE_NAME, PROP_FILE_BASE_NAME, PROP_PROJECT_NAME
	};

	public static final String			DEFAULT_TEMPLATE_ENGINE	= "stringtemplate";	//$NON-NLS-1$

	private TemplateSelectionWizardPage	templatePage;
	private TemplateParamsWizardPage	paramsPage;

	private IWorkbench					workbench;

	private WizardNewFileCreationPage	mainPage;

	private static class WrappingException extends RuntimeException {
		private static final long	serialVersionUID	= 1L;
		private final Exception		e;

		public WrappingException(Exception e) {
			this.e = e;
		}

		public Exception getWrapped() {
			return e;
		}
	}

	public BndRunFileWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(templatePage);
		addPage(mainPage);
		addPage(paramsPage);
	}

	@Override
	public boolean performFinish() {
		try {
			IFile file = mainPage.createNewFile();
			if (file == null) {
				return false;
			}

			// Open editor on new file.
			IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
			return true;
		} catch (PartInitException e) {
			ErrorDialog.openError(getShell(), "New Bnd Run File", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening editor", e));
			return true;
		} catch (WrappingException e) {
			ErrorDialog.openError(getShell(), "New Bnd Run File", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error generating file", e.getWrapped()));
			return false;
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;

		mainPage = new WizardNewFileCreationPage("newFilePage", selection) {
			@Override
			protected InputStream getInitialContents() {
				try {
					return getTemplateContents(getFileName());
				} catch (Exception e) {
					throw new WrappingException(e);
				}
			}
		};
		mainPage.setTitle("New Bnd Run Descriptor");
		mainPage.setFileExtension("bndrun"); //$NON-NLS-1$
		mainPage.setAllowExistingResources(false);

		BuiltInTemplate baseTemplate = new BuiltInTemplate("\u00abEmpty\u00bb", DEFAULT_TEMPLATE_ENGINE);
		baseTemplate.addInputResource("$fileName$", new StringResource(""));
		baseTemplate.setHelpPath("docs/empty_run.xml");

		templatePage = new TemplateSelectionWizardPage("runTemplateSelection", "bndrun", baseTemplate);
		templatePage.setTitle("Select Run Descriptor Template");

		paramsPage = new TemplateParamsWizardPage(PROPS);

		templatePage.addPropertyChangeListener(TemplateSelectionWizardPage.PROP_TEMPLATE,
			evt -> paramsPage.setTemplate(templatePage.getTemplate()));
	}

	private String baseName(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		String base = lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;
		int lastSlash = base.lastIndexOf('/');
		base = lastSlash >= 0 ? base.substring(lastSlash + 1) : base;
		return base;
	}

	private InputStream getTemplateContents(String fileName) throws Exception {
		// Load properties
		Map<String, List<Object>> params = new HashMap<>();
		params.put(PROP_FILE_NAME, Collections.<Object> singletonList(fileName));
		params.put(PROP_FILE_BASE_NAME, Collections.<Object> singletonList(baseName(fileName)));

		IPath containerPath = mainPage.getContainerFullPath();
		if (containerPath != null) {
			IResource container = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMember(containerPath);
			if (container != null) {
				String projectName = container.getProject()
					.getName();
				params.put(PROP_PROJECT_NAME, Collections.<Object> singletonList(projectName));
			}
		}

		Map<String, String> editedParams = paramsPage.getValues();
		for (Entry<String, String> editedParam : editedParams.entrySet()) {
			params.put(editedParam.getKey(), Collections.<Object> singletonList(editedParam.getValue()));
		}

		// Run the template processor
		Template template = templatePage.getTemplate();
		ResourceMap outputs;
		outputs = template.generateOutputs(params);
		Resource output = outputs.get(fileName);

		if (output == null) {
			throw new IllegalArgumentException(String.format(
				"Template error: file '%s' not found in outputs. Available names: %s", fileName, outputs.getPaths()));
		}

		// Pull the generated content
		return output.getContent();
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		IWizardPage prev = super.getPreviousPage(page);
		if (prev instanceof ISkippableWizardPage) {
			if (((ISkippableWizardPage) prev).shouldSkip()) {
				return getPreviousPage(prev);
			}
		}
		return prev;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage next = super.getNextPage(page);
		if (next instanceof ISkippableWizardPage) {
			if (((ISkippableWizardPage) next).shouldSkip()) {
				return getNextPage(next);
			}
		}
		return next;
	}
}
