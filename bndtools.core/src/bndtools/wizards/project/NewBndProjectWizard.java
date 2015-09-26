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

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.bndtools.core.ui.wizards.shared.BuiltInTemplate;
import org.bndtools.core.ui.wizards.shared.RepoTemplateSelectionWizardPage;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.Template;
import org.bndtools.templating.engine.StringTemplateEngine;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import bndtools.Plugin;

class NewBndProjectWizard extends AbstractNewBndProjectWizard {

    public static final String DEFAULT_BUNDLE_VERSION = "0.0.0.${tstamp}";

    private RepoTemplateSelectionWizardPage templatePage;

    NewBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);

    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);

        BuiltInTemplate baseTemplate = new BuiltInTemplate("\u00abEmpty\u00bb");
        baseTemplate.addResource("bnd.bnd", new StringResource(""));
        baseTemplate.setHelpPath("docs/empty_project.xml");

        templatePage = new RepoTemplateSelectionWizardPage("projectTemplateSelection", "project", workbench, baseTemplate);
        templatePage.setTitle("Select Project Template");
    }

    @Override
    public void addPages() {
        addPage(templatePage);
        addPage(pageOne);
        addPage(pageTwo);
    }

    private static void addTemplateParam(Map<String,List<Object>> params, String name, Object value) {
        List<Object> list = params.get(name);
        if (list == null) {
            list = new LinkedList<>();
            params.put(name, list);
        }
        list.add(value);
    }

    /**
     * Generate the new Bnd model for the project. This implementation simply returns an empty Bnd model.
     *
     * @param monitor
     */
    @Override
    protected void generateProjectContent(IProject project, IProgressMonitor monitor) {
        ProjectPaths bndPaths = ProjectPaths.get(ProjectLayout.BND);
        ProjectPaths projectPaths = ProjectPaths.get(pageOne.getProjectLayout());

        Map<String,List<Object>> templateParams = new HashMap<>();
        addTemplateParam(templateParams, "projectName", project.getName());
        addTemplateParam(templateParams, "basePackageDir", project.getName().replace('.', '/'));

        String projectTargetDir = projectPaths.getTargetDir();
        if (!bndPaths.getTargetDir().equals(projectTargetDir)) {
            addTemplateParam(templateParams, "targetDir", projectTargetDir);
        }

        if (ProjectLayout.MAVEN == projectPaths.getLayout()) {
            addTemplateParam(templateParams, "version", "1.0.0.SNAPSHOT");
            addTemplateParam(templateParams, "outputmask", "${@bsn}-${version;===S;${@version}}.jar");
        } else {
            addTemplateParam(templateParams, "version", DEFAULT_BUNDLE_VERSION);
        }

        Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(pageTwo.getJavaProject());
        int nr = 1;
        for (Map.Entry<String,String> entry : sourceOutputLocations.entrySet()) {
            String src = entry.getKey();
            String bin = entry.getValue();

            if (nr == 1) {
                addTemplateParam(templateParams, "srcDir", src);
                addTemplateParam(templateParams, "binDir", bin);
                nr = 2;
            } else if (nr == 2) {
                addTemplateParam(templateParams, "testSrcDir", src);
                addTemplateParam(templateParams, "testBinDir", bin);
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

        Template template = templatePage.getTemplate();
        try {
            ResourceMap outputs;
            if (template != null) {
                ResourceMap templateInputs = template.getInputSources();
                outputs = new StringTemplateEngine().generateOutputs(templateInputs, templateParams);
            } else {
                outputs = new ResourceMap(); // empty
            }

            SubMonitor progress = SubMonitor.convert(monitor, outputs.size() * 3);
            for (Entry<String,Resource> outputEntry : outputs.entries()) {
                String path = outputEntry.getKey();
                IFile file = project.getFile(path);
                mkdirs(file.getParent(), progress.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
                try (InputStream in = outputEntry.getValue().getContent()) {
                    file.create(in, 0, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                    file.setCharset(outputEntry.getValue().getTextEncoding(), progress.newChild(1));
                }
            }
        } catch (Exception e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error generating project contents from template \"{0}\".", template.getName()), e));
        }
    }

    private void mkdirs(IContainer container, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        if (container.exists())
            return;

        IContainer parent = container.getParent();
        if (parent != null)
            mkdirs(parent, progress.newChild(1));

        if (container.getType() == IResource.FOLDER) {
            IFolder folder = (IFolder) container;
            folder.create(false, true, progress.newChild(1));
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Can only create plain Folder parent containers.", null));
        }
    }

}
