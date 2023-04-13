package org.bndtools.core.ui.wizards.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.core.ui.wizards.shared.ISkippingWizard;
import org.bndtools.core.ui.wizards.shared.TemplateParamsWizardPage;
import org.bndtools.templating.Template;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.wizard.IWizardPage;

import bndtools.Plugin;
import bndtools.wizards.project.ProjectTemplateParam;

public class NewBndServiceWizard extends AbstractNewBndServiceMultiProjectWizard implements ISkippingWizard {

	public static final String						DEFAULT_BUNDLE_VERSION	= "0.0.0.${tstamp}";	//$NON-NLS-1$

	protected NewBndServiceWizardPageOne			pageOne;
	protected TemplateParamsWizardPage				paramsPage;

	protected NewBndServiceWizardNonApiProjectPage	implPage;
	protected NewBndServiceWizardNonApiProjectPage	consumerPage;
	protected String								templateName;

	protected TemplateParamsWizardPage createParamsPage() {
		return new TemplateParamsWizardPage(ProjectTemplateParam.valueStrings());
	}

	protected NewBndServiceWizardNonApiProjectPage createImplPage() {
		return new NewBndServiceWizardNonApiProjectPage(
			new NewBndServiceWizardPageOne(pageOne, ServiceTemplateConstants.DEFAULT_IMPL_SUFFIX, templateName));
	}

	protected NewBndServiceWizardNonApiProjectPage createConsumerPage() {
		return new NewBndServiceWizardNonApiProjectPage(
			new NewBndServiceWizardPageOne(pageOne, ServiceTemplateConstants.DEFAULT_CONSUMER_SUFFIX, templateName));
	}

	public NewBndServiceWizard(final NewBndServiceWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo,
		String templateName) {
		super(pageOne, pageTwo);
		this.pageOne = pageOne;
		this.templateName = templateName;
		// setup params page with normal bnd template project params
		this.paramsPage = createParamsPage();
		// setup impl page to use .impl project suffix
		this.implPage = createImplPage();
		this.consumerPage = createConsumerPage();
	}

	@SuppressWarnings("restriction")
	@Override
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		try {
			SubMonitor submonitor = SubMonitor.convert(monitor, "Creating service projects", 3);
			super.finishPage(submonitor.split(1));
			implPage.performFinish(submonitor.split(1));
			consumerPage.performFinish(submonitor.split(1));
		} finally {
			monitor.done();
		}
	}

	@Override
	public void addPages() {
		addPage(pageOne);
		addPage(paramsPage);
	}

	@Override
	protected Map<String, String> getServiceTemplateParams() {
		Map<String, String> params = new HashMap<>();
		// Service Name
		params.put(ServiceProjectTemplateParam.SERVICE_NAME.getString(), pageOne.getServiceName());
		// Package Name
		String packageName = pageOne.getPackageName();
		params.put(ProjectTemplateParam.BASE_PACKAGE_NAME.getString(), packageName);
		// api_package this is the packageName of the API package, made
		// available
		// for impl and consumer templates so that the API package can be
		// imported
		params.put(ServiceProjectTemplateParam.API_PACKAGE.getString(), packageName);
		// Package Dir
		String packageDir = packageName.replace('.', '/');
		params.put(ProjectTemplateParam.BASE_PACKAGE_DIR.getString(), packageDir);
		// Version
		params.put(ProjectTemplateParam.VERSION.getString(), DEFAULT_BUNDLE_VERSION);
		// Source Folders
		IJavaProject javaProject = pageTwo.getJavaProject();
		Map<String, String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(javaProject);
		int nr = 1;
		for (Map.Entry<String, String> entry : sourceOutputLocations.entrySet()) {
			String src = entry.getKey();
			String bin = entry.getValue();
			if (nr == 1) {
				params.put(ProjectTemplateParam.SRC_DIR.getString(), src);
				params.put(ProjectTemplateParam.BIN_DIR.getString(), bin);
				nr = 2;
			} else if (nr == 2) {
				params.put(ProjectTemplateParam.TEST_SRC_DIR.getString(), src);
				params.put(ProjectTemplateParam.TEST_BIN_DIR.getString(), bin);
				nr = 2;
			} else {
				nr++;
			}
		}

		try {
			String javaLevel = JavaProjectUtils.getJavaLevel(javaProject);
			if (javaLevel != null)
				params.put(ProjectTemplateParam.JAVA_LEVEL.getString(), javaLevel);
		} catch (Exception e) {
			Plugin.getDefault()
				.getLog()
				.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					String.format("Unable to get Java level for project %s", javaProject.getProject()
						.getName()),
					e));
		}

		Map<String, String> editedParams = paramsPage.getValues();
		for (Entry<String, String> editedEntry : editedParams.entrySet()) {
			params.put(editedEntry.getKey(), editedEntry.getValue());
		}
		return params;
	}


	@Override
	public IWizardPage getUnfilteredPreviousPage(IWizardPage page) {
		return super.getPreviousPage(page);
	}

	@Override
	public IWizardPage getUnfilteredNextPage(IWizardPage page) {
		return super.getNextPage(page);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		return ISkippingWizard.super.getPreviousPage(page);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		return ISkippingWizard.super.getNextPage(page);
	}

	@Override
	protected Template getServiceApiTemplate() {
		return pageOne.getServiceApiTemplate();
	}

	@Override
	protected Template getServiceImplTemplate() {
		return pageOne.getServiceImplTemplate();
	}

	@Override
	protected IJavaProject getServiceImplJavaProject() {
		return implPage.getJavaProject();
	}

	@Override
	protected Template getServiceConsumerTemplate() {
		return pageOne.getServiceConsumerTemplate();
	}

	@Override
	protected IJavaProject getServiceConsumerJavaProject() {
		return consumerPage.getJavaProject();
	}

}
