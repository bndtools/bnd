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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import bndtools.api.IProjectTemplate;
import bndtools.api.ProjectPaths;
import bndtools.classpath.BndContainerInitializer;

public class NewBndProjectWizardPageOne extends NewJavaProjectWizardPageOne {

    private IProjectTemplate projectTemplate;

    private final ProjectNameGroup nameGroup = new ProjectNameGroup();
    private final ProjectLocationGroup locationGroup = new ProjectLocationGroup("Location");
    @SuppressWarnings("unused")
    private final Validator fValidator;

    NewBndProjectWizardPageOne() {
        setTitle("Create a Bnd OSGi Project");

        fValidator = new Validator();

        nameGroup.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                locationGroup.setProjectName(nameGroup.getProjectName());
            }
        });

        locationGroup.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                IStatus status = locationGroup.getStatus();
                setPageComplete(status.isOK());
                if (status.isOK()) {
                    setErrorMessage(null);
                } else {
                    setErrorMessage(status.getMessage());
                }
            }
        });
    }

    @Override
    public String getProjectName() {
        return nameGroup.getProjectName();
    }

    @Override
    public URI getProjectLocationURI() {
        if (locationGroup.isLocationInWorkspace())
            return null;
        return URIUtil.toURI(locationGroup.getLocation());
    }

    @Override
    /*
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets .Composite) This has been cut
     * and pasted from the superclass because we wish to customize the contents of the page.
     */
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        final Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

        Control nameControl = nameGroup.createControl(composite);
        nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control locationControl = locationGroup.createControl(composite);
        locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control jreControl = createJRESelectionControl(composite);
        jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control workingSetControl = createWorkingSetControl(composite);
        workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control infoControl = createInfoControl(composite);
        infoControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        setControl(composite);
    }

    @Override
    public IClasspathEntry[] getDefaultClasspathEntries() {
        IClasspathEntry[] entries = super.getDefaultClasspathEntries();
        List<IClasspathEntry> result = new ArrayList<IClasspathEntry>(entries.length + 2);
        result.addAll(Arrays.asList(entries));

        // Add the Bnd classpath container entry
        IPath bndContainerPath = BndContainerInitializer.PATH_ID;
        IClasspathEntry bndContainerEntry = JavaCore.newContainerEntry(bndContainerPath, false);
        result.add(bndContainerEntry);

        return result.toArray(new IClasspathEntry[result.size()]);
    }

    @Override
    public IClasspathEntry[] getSourceClasspathEntries() {
        IPath projectPath = new Path(getProjectName()).makeAbsolute();

        List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>(2);
        newEntries.add(JavaCore.newSourceEntry(projectPath.append(ProjectPaths.PATH_SRC), null, projectPath.append(ProjectPaths.PATH_SRC_BIN)));

        if (projectTemplate == null || projectTemplate.enableTestSourceFolder())
            newEntries.add(JavaCore.newSourceEntry(projectPath.append(ProjectPaths.PATH_TEST_SRC), null, projectPath.append(ProjectPaths.PATH_TEST_BIN)));

        return newEntries.toArray(new IClasspathEntry[newEntries.size()]);
    }

    @Override
    public IPath getOutputLocation() {
        return new Path(getProjectName()).makeAbsolute().append(ProjectPaths.PATH_SRC_BIN);
    }

    public void setProjectTemplate(IProjectTemplate projectTemplate) {
        this.projectTemplate = projectTemplate;
    }

    /**
     * Validate this page and show appropriate warnings and error NewWizardMessages.
     */
    private final class Validator implements Observer {

        public void update(Observable o, Object arg) {

            final IWorkspace workspace = JavaPlugin.getWorkspace();

            final String name = null;// fNameGroup.getName();

            // check whether the project name field is empty
            if ((name == null) || (name.length() == 0)) {
                setErrorMessage(null);
                setMessage("Enter a project name.");
                setPageComplete(false);
                return;
            }

            // check whether the project name is valid
            @SuppressWarnings("unused")
            final IStatus nameStatus = workspace.validateName(name, IResource.PROJECT);
            if (!nameStatus.isOK()) {
                setErrorMessage(nameStatus.getMessage());
                setPageComplete(false);
                return;
            }

            // check whether project already exists
            final IProject handle = workspace.getRoot().getProject(name);
            if (handle.exists()) {
                setErrorMessage("A project with this name already exists.");
                setPageComplete(false);
                return;
            }

            IPath projectLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(name);
            if (projectLocation.toFile().exists()) {
                try {
                    // correct casing
                    String canonicalPath = projectLocation.toFile().getCanonicalPath();
                    projectLocation = new Path(canonicalPath);
                } catch (IOException e) {
                    JavaPlugin.log(e);
                }

                String existingName = projectLocation.lastSegment();
                String newName = null; // fNameGroup.getName();
                if (!existingName.equals(newName)) {
                    setErrorMessage(Messages.format("The name of the new project must be ''{0}''", BasicElementLabels.getResourceName(existingName)));
                    setPageComplete(false);
                    return;
                }

            }

            final String location = null;// fLocationGroup.getLocation().toOSString();

            // check whether location is empty
            if ((location == null) || (location.length() == 0)) {
                setErrorMessage(null);
                setMessage("Enter a location for the project.");
                setPageComplete(false);
                return;
            }

            // check whether the location is a syntactically correct path
            if (!Path.EMPTY.isValidPath(location)) {
                setErrorMessage("Invalid project contents directory");
                setPageComplete(false);
                return;
            }

            IPath projectPath = Path.fromOSString(location);

            // if (fLocationGroup.isWorkspaceRadioSelected())
            // projectPath= projectPath.append(fNameGroup.getName());

            if (projectPath.toFile().exists()) {// create from existing source
                if (Platform.getLocation().isPrefixOf(projectPath)) { // create
                                                                      // from
                                                                      // existing
                                                                      // source
                                                                      // in
                                                                      // workspace
                    if (!Platform.getLocation().equals(projectPath.removeLastSegments(1))) {
                        setErrorMessage("Projects located in the workspace folder must be direct sub folders of the workspace folder");
                        setPageComplete(false);
                        return;
                    }

                    if (!projectPath.toFile().exists()) {
                        setErrorMessage("The selected existing source location in the workspace root does not exist");
                        setPageComplete(false);
                        return;
                    }
                }
                // } else if (!fLocationGroup.isWorkspaceRadioSelected())
                // {//create at non existing external location
                // if (!canCreate(projectPath.toFile())) {
                // setErrorMessage(NewWizardMessages.NewJavaProjectWizardPageOne_Message_cannotCreateAtExternalLocation);
                // setPageComplete(false);
                // return;
                // }
                //
                // // If we do not place the contents in the workspace validate
                // the
                // // location.
                // final IStatus locationStatus=
                // workspace.validateProjectLocation(handle, projectPath);
                // if (!locationStatus.isOK()) {
                // setErrorMessage(locationStatus.getMessage());
                // setPageComplete(false);
                // return;
                // }
            }

            setPageComplete(true);

            setErrorMessage(null);
            setMessage(null);
        }

        // private boolean canCreate(File file) {
        // while (!file.exists()) {
        // file= file.getParentFile();
        // if (file == null)
        // return false;
        // }
        //
        // return file.canWrite();
        // }
    }

}
