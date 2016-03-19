/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.wizards.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.bndtools.core.ui.wizards.shared.BuiltInTemplate;
import org.bndtools.core.ui.wizards.shared.TemplateSelectionWizardPage;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.Template;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import aQute.bnd.build.Project;
import bndtools.Plugin;

class NewBndProjectWizard extends AbstractNewBndProjectWizard {

    public static final String DEFAULT_TEMPLATE_ENGINE = "stringtemplate"; //$NON-NLS-1$
    public static final String DEFAULT_BUNDLE_VERSION = "0.0.0.${tstamp}"; //$NON-NLS-1$
    public static final String EMPTY_TEMPLATE_NAME = "\u00abEmpty\u00bb";

    private TemplateSelectionWizardPage templatePage;

    NewBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);

        BuiltInTemplate baseTemplate = new BuiltInTemplate(EMPTY_TEMPLATE_NAME, DEFAULT_TEMPLATE_ENGINE);
        baseTemplate.addInputResource(Project.BNDFILE, new StringResource("")); //$NON-NLS-1$
        baseTemplate.setHelpPath("docs/empty_project.xml"); //$NON-NLS-1$

        templatePage = new TemplateSelectionWizardPage("projectTemplateSelection", "project", baseTemplate);
        templatePage.setTitle("Select Project Template");

        templatePage.addPropertyChangeListener(TemplateSelectionWizardPage.PROP_TEMPLATE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                pageOne.setTemplate(templatePage.getTemplate());
            }
        });
    }

    @Override
    public void addPages() {
        addPage(templatePage);
        addPage(pageOne);
        addPage(pageTwo);
    }

    @Override
    protected Map<String,String> getProjectTemplateParams() {
        Map<ProjectTemplateParam,String> params = new HashMap<>();
        params.put(ProjectTemplateParam.PROJECT_NAME, pageOne.getProjectName());
        String packageName = pageOne.getPackageName();
        params.put(ProjectTemplateParam.BASE_PACKAGE_NAME, packageName);
        String packageDir = packageName.replace('.', '/');
        params.put(ProjectTemplateParam.BASE_PACKAGE_DIR, packageDir);

        ProjectPaths bndPaths = ProjectPaths.get(ProjectLayout.BND);
        ProjectPaths projectPaths = ProjectPaths.get(pageOne.getProjectLayout());

        String projectTargetDir = projectPaths.getTargetDir();
        if (!bndPaths.getTargetDir().equals(projectTargetDir)) {
            params.put(ProjectTemplateParam.TARGET_DIR, projectTargetDir);
        }

        if (ProjectLayout.MAVEN == projectPaths.getLayout()) {
            params.put(ProjectTemplateParam.VERSION, "1.0.0.SNAPSHOT");
            params.put(ProjectTemplateParam.VERSION_OUTPUTMASK, "${@bsn}-${version;===S;${@version}}.jar");
        } else {
            params.put(ProjectTemplateParam.VERSION, DEFAULT_BUNDLE_VERSION);
        }

        Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(pageTwo.getJavaProject());
        int nr = 1;
        for (Map.Entry<String,String> entry : sourceOutputLocations.entrySet()) {
            String src = entry.getKey();
            String bin = entry.getValue();

            if (nr == 1) {
                params.put(ProjectTemplateParam.SRC_DIR, src);
                params.put(ProjectTemplateParam.BIN_DIR, bin);
                nr = 2;
            } else if (nr == 2) {
                params.put(ProjectTemplateParam.TEST_SRC_DIR, src);
                params.put(ProjectTemplateParam.TEST_BIN_DIR, bin);
                nr = 2;
            } else {
                // if for some crazy reason we end up with more than 2 paths, we log them in
                // extension properties (we cannot write comments) but this should never happen
                // anyway since the second page will not complete if there are not exactly 2 paths
                // so this could only happen if someone adds another page (that changes them again)

                // TODO
                // model.genericSet("X-WARN-" + nr, "Ignoring source path " + src + " -> " + bin);
                nr++;
            }
        }

        Map<String,String> params_ = new HashMap<>();
        for (Entry<ProjectTemplateParam,String> entry : params.entrySet())
            params_.put(entry.getKey().getString(), entry.getValue());
        return params_;
    }

    /**
     * Generate the new Bnd model for the project. This implementation simply returns an empty Bnd model.
     *
     * @param monitor
     */
    @Override
    protected void generateProjectContent(IProject project, IProgressMonitor monitor, Map<String,String> params) {
        Map<String,List<Object>> templateParams = new HashMap<>();
        for (Entry<String,String> param : params.entrySet()) {
            templateParams.put(param.getKey(), Collections.<Object> singletonList(param.getValue()));
        }

        Template template = templatePage.getTemplate();
        try {
            ResourceMap outputs;
            if (template != null) {
                outputs = template.generateOutputs(templateParams);
            } else {
                outputs = new ResourceMap(); // empty
            }

            SubMonitor progress = SubMonitor.convert(monitor, outputs.size() * 3);
            for (Entry<String,Resource> outputEntry : outputs.entries()) {
                String path = outputEntry.getKey();
                Resource resource = outputEntry.getValue();

                switch (resource.getType()) {
                case Folder :
                    IFolder folder = project.getFolder(path);
                    FileUtils.mkdirs(folder, progress.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
                    break;
                case File :
                    IFile file = project.getFile(path);
                    FileUtils.mkdirs(file.getParent(), progress.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
                    try (InputStream in = resource.getContent()) {
                        if (file.exists())
                            file.setContents(in, 0, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                        else
                            file.create(in, 0, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                        file.setCharset(resource.getTextEncoding(), progress.newChild(1));
                    }
                    break;
                default :
                    throw new IllegalArgumentException("Unknown resource type " + resource.getType());
                }
            }
        } catch (Exception e) {
            String message = MessageFormat.format("Error generating project contents from template \"{0}\".", template != null ? template.getName() : "<null>");
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, e));
        }
    }

}
