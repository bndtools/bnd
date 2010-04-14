package aQute.bnd.junit;


import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import aQute.bnd.plugin.*;

public class OSGiArgumentsTab extends AbstractLaunchConfigurationTab {
    public static final String ATTR_KEEP = Activator.PLUGIN_ID
                                                        + ".CLEARCACHE"; //$NON-NLS-1$
    public static final String ATTR_REPORT   = Activator.PLUGIN_ID
                                                        + ".REPORT"; //$NON-NLS-1$

    Button                     wReport;
    private Button             wKeep;

    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayout(new FormLayout());
        setControl(comp);

        wKeep = new Button(comp, SWT.CHECK);
        final FormData fd_wClearCache = new FormData();
        fd_wClearCache.bottom = new FormAttachment(wKeep, 25, SWT.BOTTOM);
        fd_wClearCache.top = new FormAttachment(wKeep, 5, SWT.BOTTOM);
        fd_wClearCache.right = new FormAttachment(wKeep, 189, SWT.LEFT);
        fd_wClearCache.left = new FormAttachment(wKeep, 0, SWT.LEFT);
        wKeep.setLayoutData(fd_wClearCache);
        wKeep.setText("Keep");

        wReport = new Button(comp, SWT.CHECK);
        final FormData fd_verboseButton = new FormData();
        fd_verboseButton.top = new FormAttachment(wReport, 5, SWT.BOTTOM);
        fd_verboseButton.left = new FormAttachment(wReport, 0, SWT.LEFT);
        wReport.setLayoutData(fd_verboseButton);
        wReport.setText("Report");

        validatePage();

    }

    private void validatePage() {
        setErrorMessage(null);
        setMessage(null);
    }

    public String getName() {
        return "OSGi";
    }

    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            boolean keep = configuration.getAttribute(ATTR_KEEP,
                    false);
            wKeep.setSelection(keep);
            boolean report = configuration.getAttribute(ATTR_REPORT,
                    false);
            wReport.setSelection(report);
        } catch (Exception ce) {
            ce.printStackTrace();
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(ATTR_REPORT, wReport.getSelection());
        configuration
                .setAttribute(ATTR_KEEP, wKeep.getSelection());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        // TODO Auto-generated method stub

    }

    /**
     * Returns the Java project specified by the given launch configuration, or
     * <code>null</code> if none.
     * 
     * @param configuration
     *            launch configuration
     * @return the Java project specified by the given launch configuration, or
     *         <code>null</code> if none
     * @exception CoreException
     *                if unable to retrieve the attribute
     */
    public IJavaProject getJavaProject(ILaunchConfiguration configuration)
            throws CoreException {
        String projectName = getJavaProjectName(configuration);
        if (projectName != null) {
            projectName = projectName.trim();
            if (projectName.length() > 0) {
                IProject project = ResourcesPlugin.getWorkspace().getRoot()
                        .getProject(projectName);
                IJavaProject javaProject = JavaCore.create(project);
                if (javaProject != null && javaProject.exists()) {
                    return javaProject;
                }
            }
        }
        return null;
    }

    /**
     * Returns the Java project name specified by the given launch
     * configuration, or <code>null</code> if none.
     * 
     * @param configuration
     *            launch configuration
     * @return the Java project name specified by the given launch
     *         configuration, or <code>null</code> if none
     * @exception CoreException
     *                if unable to retrieve the attribute
     */
    public String getJavaProjectName(ILaunchConfiguration configuration)
            throws CoreException {
        return configuration.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                (String) null);
    }

}
