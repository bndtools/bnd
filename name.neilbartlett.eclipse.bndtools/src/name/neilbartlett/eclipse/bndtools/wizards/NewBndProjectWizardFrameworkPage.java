package name.neilbartlett.eclipse.bndtools.wizards;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import name.neilbartlett.eclipse.bndtools.classpath.FrameworkClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class NewBndProjectWizardFrameworkPage extends WizardPage {
	
	private final FrameworkSelector frameworkSelector = new FrameworkSelector();
	
	private IFrameworkInstance framework = null;
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
		frameworkSelector.createControl(grpFramework);
		
		Group grpExtras = new Group(composite, SWT.NONE);
		grpExtras.setText("Extra Compilation Libraries");
		final Button annotationsCheck = new Button(grpExtras, SWT.CHECK);
		annotationsCheck.setText("Include Bnd Annotations library");
		
		// Init controls
		frameworkSelector.setSelectedFramework(framework);
		annotationsCheck.setSelection(includeAnnotations);
		
		// Listeners
		frameworkSelector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(FrameworkSelector.PROP_SELECTED_FRAMEWORK.equals(evt.getPropertyName())) {
					framework = frameworkSelector.getSelectedFramework();
				}
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
		gd.heightHint = 200;
		grpFramework.setLayoutData(gd);
		grpFramework.setLayout(new FillLayout());
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		grpExtras.setLayoutData(gd);
		grpExtras.setLayout(new FillLayout());

		setControl(composite);
	}
	
	@Override
	public String getErrorMessage() {
		return frameworkSelector.getErrorMessage();
	}
	@Override
	public boolean isPageComplete() {
		return framework != null && frameworkSelector.getErrorMessage() == null;
	}
	
	public IClasspathEntry getFrameworkClasspathEntry() {
		if(framework == null)
			return null;
		
		IPath path = new Path(FrameworkClasspathContainerInitializer.FRAMEWORK_CONTAINER_ID);
		path = path.append(framework.getFrameworkId());
		
		IPath instancePath = framework.getInstancePath();
		String encodedPath;
		try {
			encodedPath = URLEncoder.encode(instancePath.toString(), "UTF-8");  //$NON-NLS-1$
			path = path.append(encodedPath);
			
			if(includeAnnotations) {
				path = path.append(FrameworkClasspathContainerInitializer.PROP_ANNOTATIONS_LIB + "=true");
			}
			
			return JavaCore.newContainerEntry(path);
		} catch (UnsupportedEncodingException e) {
			// TODO
			e.printStackTrace();
			return null;
		}
	}
}
