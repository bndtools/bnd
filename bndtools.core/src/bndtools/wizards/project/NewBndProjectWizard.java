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

import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import bndtools.Plugin;
import bndtools.editor.model.BndProject;

class NewBndProjectWizard extends AbstractNewBndProjectWizard {

    public static final String DEFAULT_BUNDLE_VERSION = "0.0.0.${tstamp}";

    private final TemplateSelectionWizardPage templatePage = new TemplateSelectionWizardPage();

    NewBndProjectWizard(final NewBndProjectWizardPageOne pageOne, final NewJavaProjectWizardPageTwo pageTwo) {
        super(pageOne, pageTwo);

        templatePage.addPropertyChangeListener(TemplateSelectionWizardPage.PROP_TEMPLATE, new PropertyChangeListener() {
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
        BndEditModel model = super.generateBndModel(monitor);

        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            model.setBundleVersion(DEFAULT_BUNDLE_VERSION);
            template.modifyInitialBndModel(model);
        }
        try {
            String name = pageTwo.getJavaProject().getProject().getName();
            IPath projectPath = new Path(name).makeAbsolute();
            IClasspathEntry[] entries = pageTwo.getJavaProject().getResolvedClasspath(true);
            int nr = 1;
            for (IClasspathEntry entry : entries) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath srcPath = entry.getPath();
                    IPath src = srcPath.makeRelativeTo(projectPath);
                    IPath srcOutPath = entry.getOutputLocation();
                    if (srcOutPath == null) {
                        srcOutPath = pageTwo.getJavaProject().getOutputLocation();
                    }
                    IPath srcOut = srcOutPath.makeRelativeTo(projectPath);
                    if (nr == 1) {
                        if (!ProjectPaths.PATH_SRC.equals(src.toString())) {
                            model.genericSet("src", src.toString());
                        }
                        if (!ProjectPaths.PATH_SRC_BIN.equals(srcOut.toString())) {
                            model.genericSet("bin", srcOut.toString());
                        }
                        nr = 2;
                    } else if (nr == 2) {
                        if (!ProjectPaths.PATH_TEST_SRC.equals(src.toString())) {
                            model.genericSet("testsrc", src.toString());
                        }
                        if (!ProjectPaths.PATH_TEST_BIN.equals(srcOut.toString())) {
                            model.genericSet("testbin", srcOut.toString());
                        }
                        nr = 2;
                    } else {
                        // if for some crazy reason we end up with more than 2 paths, we log them in
                        // extension properties (we cannot write comments) but this should never happen
                        // anyway since the second page will not complete if there are not exactly 2 paths
                        // so this could only happen if someone adds another page (that changes them again)
                        model.genericSet("X-WARN-" + nr, "Ignoring source path " + src + " -> " + srcOut);
                        nr++;
                    }
                }
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

        IProjectTemplate template = templatePage.getTemplate();
        if (template != null) {
            template.modifyInitialBndProject(proj);
        }

        return proj;
    }

}
