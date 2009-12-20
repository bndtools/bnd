package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class FrameworkTypeWizardPage extends WizardPage {

	public static final String PROP_FRAMEWORK_TYPE = "frameworkType";

	private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	private Shell parentShell;
	private TableViewer viewer;
	
	private IFramework framework = null;
	
	public FrameworkTypeWizardPage() {
		super("frameworkType");
	}

	public void createControl(Composite parent) {
		parentShell = parent.getShell();

		setTitle("Add OSGi Framework Instance");
		setMessage("Select one of the supported OSGi framework types.");

		Composite composite = new Composite(parent, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText("Supported OSGi Framework Types:");
		Table table = new Table(composite, SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		
		// Initialise
		viewer.setInput(Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_OSGI_FRAMEWORKS));
		viewer.setLabelProvider(new FrameworkConfigurationElementLabelProvider(parent.getDisplay()));
		
		// Add events
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				getFrameworkFromSelection();
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				getFrameworkFromSelection();
				getContainer().showPage(getNextPage());
			}
		});
		
		// Layout
		composite.setLayout(new GridLayout(1, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setControl(composite);
	}
	
	@Override
	public boolean isPageComplete() {
		return framework != null;
	}
	
	void getFrameworkFromSelection() {
		IFramework oldFramework = this.framework;
		
		IConfigurationElement configElem = (IConfigurationElement) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if(configElem == null) {
			this.framework = null;
		} else {
			try {
				this.framework = (IFramework) configElem.createExecutableExtension("class");
			} catch (CoreException e) {
				ErrorDialog.openError(parentShell, "Error", null, e.getStatus());
				this.framework = null;
			}
		}
		
		if(oldFramework != framework)
			propertySupport.firePropertyChange(PROP_FRAMEWORK_TYPE, oldFramework, this.framework);
		
		getContainer().updateButtons();
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

	static class FrameworkConfigurationElementLabelProvider extends LabelProvider {
		
		private final Device device;
		private final Map<String, Image> images = new HashMap<String, Image>();

		public FrameworkConfigurationElementLabelProvider(Device device) {
			this.device = device;
		}
		
		@Override
		public String getText(Object element) {
			IConfigurationElement configElem = (IConfigurationElement) element;
			
			return configElem.getAttribute("displayName");
		}
		
		@Override
		public Image getImage(Object element) {
			IConfigurationElement configElem = (IConfigurationElement) element;
			
			String pluginId = configElem.getContributor().getName();
			String iconPath = configElem.getAttribute("icon");
			String key = pluginId + "/" + iconPath;
			
			Image image = images.get(key);
			if(image == null) {
				ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconPath);
				image = descriptor != null ? descriptor.createImage(false, device) : null;
				images.put(key, image);
			}
			return image;
		}
		
		@Override
		public void dispose() {
			super.dispose();
			for (Entry<String, Image> entry : images.entrySet()) {
				entry.getValue().dispose();
			}
		}
	}

}
