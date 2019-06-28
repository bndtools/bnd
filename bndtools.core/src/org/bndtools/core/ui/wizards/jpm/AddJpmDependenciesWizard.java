package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.lib.exceptions.Exceptions;
import bndtools.Plugin;

public class AddJpmDependenciesWizard extends Wizard {

	private final JpmDependencyWizardPage	depsPage;
	private final Set<ResourceDescriptor>	result	= new HashSet<>();

	public AddJpmDependenciesWizard(URI uri) {
		this.depsPage = new JpmDependencyWizardPage(uri);

		setNeedsProgressMonitor(true);
		addPage(depsPage);
	}

	@Override
	public boolean performFinish() {
		result.clear();

		result.addAll(depsPage.getDirectResources());
		result.addAll(depsPage.getSelectedIndirectResources());

		final Set<ResourceDescriptor> indirectResources;
		if (depsPage.getIndirectResources() != null) {
			indirectResources = new HashSet<>(depsPage.getIndirectResources());
			indirectResources.removeAll(result);
		} else {
			indirectResources = Collections.<ResourceDescriptor> emptySet();
		}
		final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0,
			"Errors occurred while processing dependencies.", null);
		IRunnableWithProgress runnable = monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor, result.size() + indirectResources.size());
			progress.setTaskName("Processing dependencies...");

			// Process all resources (including non-selected ones) into the
			// repository
			for (ResourceDescriptor resource1 : result)
				processResource(resource1, status, progress.newChild(1));
			for (ResourceDescriptor resource2 : indirectResources)
				processResource(resource2, status, progress.newChild(1));
		};

		try {
			getContainer().run(true, true, runnable);

			if (!status.isOK())
				ErrorDialog.openError(getShell(), "Errors", null, status);

			return true;
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Error", Exceptions.unrollCause(e, InvocationTargetException.class)
				.getMessage());
			return false;
		} catch (InterruptedException e) {
			return false;
		}
	}

	public Set<ResourceDescriptor> getResult() {
		return Collections.unmodifiableSet(result);
	}

	@SuppressWarnings("unused")
	private void processResource(ResourceDescriptor resource, MultiStatus status, IProgressMonitor monitor) {
		SearchableRepository repo = depsPage.getRepository();
		try {
			if (!resource.included)
				repo.addResource(resource);
		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				"Error adding resource to local Searchable Repository: " + resource.bsn + " [" + resource.version + "]",
				e));
		}
	}

}
