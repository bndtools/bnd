package name.neilbartlett.eclipse.bndtools.wizards;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import name.neilbartlett.eclipse.bndtools.classpath.FrameworkClasspathContainer;
import name.neilbartlett.eclipse.bndtools.classpath.FrameworkClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.sun.jdi.connect.Connector.SelectedArgument;

public class NewBndProjectWizardFrameworkPage extends WizardPage {
	
	private final FrameworkSelector selector = new FrameworkSelector();
	
	private boolean useSpec = true;
	private OSGiSpecLevel specLevel = null;
	private IFrameworkInstance frameworkInstance = null;
	private boolean includeAnnotations = true;
	
	NewBndProjectWizardFrameworkPage() {
		super("NewBndProjectWizardFrameworkPage");
		setTitle("Add OSGi Framework.");
		setDescription("Add an OSGi Framework to the project class path.");
	}
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		Group grpFramework = new Group(composite, SWT.NONE);
		grpFramework.setText("Installed Frameworks");
		selector.createControl(grpFramework);
		
		final Button annotationsCheck = new Button(grpFramework, SWT.CHECK);
		annotationsCheck.setText("Include Bnd Annotations library");
		
		// Init controls
		selector.setUseSpecLevel(useSpec);
		selector.setSelection(useSpec ? specLevel : frameworkInstance);
		annotationsCheck.setSelection(includeAnnotations);
		
		// Listeners
		selector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				useSpec = selector.isUseSpecLevel();
				specLevel = selector.getSelectedSpecLevel();
				frameworkInstance = selector.getSelectedFramework();
				
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		annotationsCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeAnnotations = annotationsCheck.getSelection();
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		// Layout
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, false));
		
		GridData gd;
		gd = new GridData(GridData.FILL_HORIZONTAL);
		grpFramework.setLayoutData(gd);
		grpFramework.setLayout(new GridLayout(1, false));

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 200;
		selector.getControl().setLayoutData(gd);

		setControl(composite);
	}
	
	@Override
	public String getErrorMessage() {
		return selector.getErrorMessage();
	}
	@Override
	public boolean isPageComplete() {
		boolean result = selector.getErrorMessage() == null;
		result &= (useSpec && specLevel != null) || frameworkInstance != null;
		return result;
	}
	
	public IClasspathEntry getFrameworkClasspathEntry() {
		FrameworkClasspathContainer classpathContainer;
		if(useSpec) {
			classpathContainer = FrameworkClasspathContainer.createForSpecLevel(specLevel, includeAnnotations);
		} else {
			classpathContainer = FrameworkClasspathContainer.createForSpecificFramework(frameworkInstance, includeAnnotations);
		}
		
		IPath path = FrameworkClasspathContainerInitializer.createPathForContainer(classpathContainer);
		return JavaCore.newContainerEntry(path);
	}
}
