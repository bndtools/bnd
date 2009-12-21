package name.neilbartlett.eclipse.bndtools.prefs.frameworks.ui;

import java.util.ArrayList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.service.prefs.BackingStoreException;

public class CreateFrameworkInstanceWizard extends Wizard {
	
	private final List<IFrameworkInstance> instances;
	private FrameworkTypeWizardPage frameworkTypePage;
	private FrameworkPathWizardPage frameworkPathPage;

	public CreateFrameworkInstanceWizard(List<IFrameworkInstance> instances) {
		this.instances = instances;
	}

	@Override
	public boolean performFinish() {
		List<IFrameworkInstance> newInstances = new ArrayList<IFrameworkInstance>(instances.size() + 1);
		
		newInstances.addAll(instances);
		newInstances.add(frameworkPathPage.getInstance());
		
		try {
			FrameworkPreferencesInitializer.saveFrameworkInstancesList(newInstances);
			return true;
		} catch (BackingStoreException e) {
			Status status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error saving preferences.", e);
			ErrorDialog.openError(getShell(), "Error", null, status);
			return false;
		}
	}
	
	@Override
	public void addPages() {
		frameworkTypePage = new FrameworkTypeWizardPage();
		frameworkPathPage = new FrameworkPathWizardPage(instances);
		
		frameworkTypePage.addPropertyChangeListener(FrameworkTypeWizardPage.PROP_FRAMEWORK_TYPE, frameworkPathPage);
		
		addPage(frameworkTypePage);
		addPage(frameworkPathPage);
	}

}
