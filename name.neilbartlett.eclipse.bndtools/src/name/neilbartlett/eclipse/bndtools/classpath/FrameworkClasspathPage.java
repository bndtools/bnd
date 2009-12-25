package name.neilbartlett.eclipse.bndtools.classpath;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class FrameworkClasspathPage extends WizardPage implements
		IClasspathContainerPage {
	
	private final FrameworkSelector selector = new FrameworkSelector();

	public FrameworkClasspathPage() {
		super("frameworkClasspathPage");
	}

	public boolean finish() {
		return selector.getSelectedFramework() != null;
	}

	public IClasspathEntry getSelection() {
		IPath path = new Path(FrameworkClasspathContainerInitializer.FRAMEWORK_CONTAINER_ID);
		IFrameworkInstance selectedFramework = selector.getSelectedFramework();
		path = path.append(selectedFramework.getFrameworkId());
		
		IPath instancePath = selectedFramework.getInstancePath();
		String encodedPath;
		try {
			encodedPath = URLEncoder.encode(instancePath.toString(), "UTF-8");  //$NON-NLS-1$
			path = path.append(encodedPath);
			return JavaCore.newContainerEntry(path);
		} catch (UnsupportedEncodingException e) {
			// TODO
			e.printStackTrace();
			return null;
		}
	}

	public void setSelection(IClasspathEntry containerEntry) {
		try {
			if(containerEntry == null) {
				selector.setSelectedFramework(null);
			} else {
				IPath containerPath = containerEntry.getPath();
				selector.setSelectedFramework(FrameworkClasspathContainerInitializer.getFrameworkInstanceForContainerPath(containerPath));
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "Unable to set classpath selection", e.getStatus());
		}
	}
	
	public void createControl(Composite parent) {
		setTitle("OSGi Framework");
		setMessage("Select an OSGi framework instance.");
		
		selector.createControl(parent);
		selector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});

		Control selectorControl = selector.getControl();
		selectorControl.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		setControl(selectorControl);
	}
	
	@Override
	public String getErrorMessage() {
		return selector.getErrorMessage();
	}
}
