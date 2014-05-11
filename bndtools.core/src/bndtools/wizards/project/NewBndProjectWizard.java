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
import java.text.MessageFormat;
import java.util.Map;

import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.model.BndProject;

class NewBndProjectWizard extends AbstractNewBndProjectWizard {

    public static final String DEFAULT_BUNDLE_VERSION = "0.0.0.${tstamp}";

    private final TemplateSelectionWizardPage templatePage = new TemplateSelectionWizardPage();

    NewBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);

        templatePage.addPropertyChangeListener(TemplateSelectionWizardPage.PROP_TEMPLATE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                pageOne.setProjectTemplate((IProjectTemplate) evt.getNewValue());
            }
        });
    }

    @Override
    public void addPages() {
        addPage(pageOne);
        addPage(templatePage);
        addPage(pageTwo);
    }

    /**
     * Generate the new Bnd model for the project. This implementation simply returns an empty Bnd model.
     *
     * @param monitor
     */
    @Override
    protected BndEditModel generateBndModel(IProgressMonitor monitor) {
        ProjectPaths bndPaths = ProjectPaths.get(ProjectLayout.BND);
        BndEditModel model = super.generateBndModel(monitor);

        ProjectPaths projectPaths = ProjectPaths.get(pageOne.getProjectLayout());

        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            String name = pageTwo.getJavaProject().getProject().getName();
            model.setBundleVersion(DEFAULT_BUNDLE_VERSION);
            template.modifyInitialBndModel(model, name, projectPaths);
        }
        try {
            Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(pageTwo.getJavaProject());
            if (sourceOutputLocations != null) {
                int nr = 1;
                for (Map.Entry<String,String> entry : sourceOutputLocations.entrySet()) {
                    String src = entry.getKey();
                    String bin = entry.getValue();

                    if (nr == 1) {
                        if (!bndPaths.getSrc().equals(src)) {
                            model.genericSet(Constants.DEFAULT_PROP_SRC_DIR, src);
                        }
                        if (!bndPaths.getBin().equals(bin)) {
                            model.genericSet(Constants.DEFAULT_PROP_BIN_DIR, bin);
                        }
                        nr = 2;
                    } else if (nr == 2) {
                        if (!bndPaths.getTestSrc().equals(src)) {
                            model.genericSet(Constants.DEFAULT_PROP_TESTSRC_DIR, src);
                        }
                        if (!bndPaths.getTestBin().equals(bin)) {
                            model.genericSet(Constants.DEFAULT_PROP_TESTBIN_DIR, bin);
                        }
                        nr = 2;
                    } else {
                        // if for some crazy reason we end up with more than 2 paths, we log them in
                        // extension properties (we cannot write comments) but this should never happen
                        // anyway since the second page will not complete if there are not exactly 2 paths
                        // so this could only happen if someone adds another page (that changes them again)
                        model.genericSet("X-WARN-" + nr, "Ignoring source path " + src + " -> " + bin);
                        nr++;
                    }
                }
            }

            String projectTargetDir = projectPaths.getTargetDir();
            if (!bndPaths.getTargetDir().equals(projectTargetDir)) {
                model.genericSet(Constants.DEFAULT_PROP_TARGET_DIR, projectTargetDir);
            }

            if (ProjectLayout.MAVEN == projectPaths.getLayout()) {
                model.setBundleVersion("1.0.0.SNAPSHOT");
                model.genericSet(Constants.OUTPUTMASK, "${@bsn}-${version;===S;${@version}}.jar");
            }
        } catch (Exception e) {
            ErrorDialog.openError(getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error setting paths in Bnd project descriptor file ({0}).", Project.BNDFILE), e));
        }

        return model;
    }

    /**
     * Allows for an IProjectTemplate to modify the new Bnd project
     *
     * @param monitor
     */
    @Override
    protected BndProject generateBndProject(IProject project, IProgressMonitor monitor) {
        BndProject proj = super.generateBndProject(project, monitor);

        ProjectPaths projectPaths = ProjectPaths.get(pageOne.getProjectLayout());
        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            String name = pageTwo.getJavaProject().getProject().getName();
            template.modifyInitialBndProject(proj, name, projectPaths);
        }

        return proj;
    }

}
