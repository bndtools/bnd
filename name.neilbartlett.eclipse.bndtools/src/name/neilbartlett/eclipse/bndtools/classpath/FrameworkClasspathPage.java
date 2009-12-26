package name.neilbartlett.eclipse.bndtools.classpath;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.ui.FrameworkSelector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class FrameworkClasspathPage extends WizardPage implements
		IClasspathContainerPage {
	
	private final FrameworkSelector selector = new FrameworkSelector();
	private boolean includeAnnotations = true;

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

	public void setSelection(IClasspathEntry containerEntry) {
		if(containerEntry == null) {
			selector.setSelectedFramework(null);
		} else {
			IPath containerPath = containerEntry.getPath();
			IFrameworkInstance frameworkInstance = FrameworkClasspathContainerInitializer.getFrameworkInstanceForContainerPath(containerPath);
			selector.setSelectedFramework(frameworkInstance);
			
			Map<String, String> properties = FrameworkClasspathContainerInitializer.getPropertiesForContainerPath(containerPath);
			if(properties != null && Boolean.TRUE.toString().equals(properties.get(FrameworkClasspathContainerInitializer.PROP_ANNOTATIONS_LIB))) {
				includeAnnotations = true;
			} else {
				includeAnnotations = false;
			}
		}
	}
	
	public void createControl(Composite parent) {
		setTitle("OSGi Framework");
		
		Composite composite = new Composite(parent, SWT.NONE);
		
		Group grpFramework = new Group(composite, SWT.NONE);
		grpFramework.setText("Installed Frameworks");
		selector.createControl(grpFramework);
		Control selectorControl = selector.getControl();
		
		Group grpExtras = new Group(composite, SWT.NONE);
		grpExtras.setText("Extra Compilation Libraries");
		final Button annotationsCheck = new Button(grpExtras, SWT.CHECK);
		annotationsCheck.setText("Include Bnd Annotations library");
		
		// Initialise
		annotationsCheck.setSelection(includeAnnotations);
		
		// Events
		selector.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		annotationsCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				includeAnnotations = annotationsCheck.getSelection();
			}
		});

		// Layout
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, false));
		
		GridData grpFrameworkLayoutData = new GridData(GridData.FILL_HORIZONTAL);
		grpFrameworkLayoutData.heightHint = 200;
		grpFramework.setLayoutData(grpFrameworkLayoutData);
		
		grpFramework.setLayout(new GridLayout(1, false));
		selectorControl.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		grpExtras.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		grpExtras.setLayout(new GridLayout(1, false));
		
		setControl(composite);
	}
	
	@Override
	public String getErrorMessage() {
		return selector.getErrorMessage();
	}
}
