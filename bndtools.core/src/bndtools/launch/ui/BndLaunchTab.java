package bndtools.launch.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class BndLaunchTab extends AbstractLaunchConfigurationTab {

    private Image image = null;

    private final ProjectLaunchTabPiece projectPiece = new ProjectLaunchTabPiece();
    private final FrameworkLaunchTabPiece frameworkPiece = new FrameworkLaunchTabPiece();
    private final LoggingLaunchTabPiece loggingPiece = new LoggingLaunchTabPiece();

    private final PropertyChangeListener updateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            checkValid();
            updateLaunchConfigurationDialog();
        }
    };

    @Override
    protected boolean isDirty() {
        return projectPiece.isDirty() || frameworkPiece.isDirty() || loggingPiece.isDirty();
    };

	public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        Control projectControl = projectPiece.createControl(composite);
        Control frameworkControl = frameworkPiece.createControl(composite);
        Control loggingControl = loggingPiece.createControl(composite);

        // Listeners
        projectPiece.addPropertyChangeListener(updateListener);
        frameworkPiece.addPropertyChangeListener(updateListener);
        loggingPiece.addPropertyChangeListener(updateListener);

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, true);
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        projectControl.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        frameworkControl.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        loggingControl.setLayoutData(gd);
	}

    void checkValid() {
        String error = projectPiece.checkForError();
        if(error != null) {
            setErrorMessage(error);
            return;
        }
        setErrorMessage(null);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        projectPiece.setDefaults(configuration);
        frameworkPiece.setDefaults(configuration);
        loggingPiece.setDefaults(configuration);
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            projectPiece.initializeFrom(configuration);
            frameworkPiece.initializeFrom(configuration);
            loggingPiece.initializeFrom(configuration);
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configuration.", e));
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        projectPiece.performApply(configuration);
        frameworkPiece.performApply(configuration);
        loggingPiece.performApply(configuration);
    }

    public String getName() {
        return "Main";
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
            }
        }
        return image;
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (this) {
            if (image != null)
                image.dispose();
        }
    }
}