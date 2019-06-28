package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.service.repository.SearchableRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class QueryJpmDependenciesRunnable implements IRunnableWithProgress {

	private final URI					origin;
	private final SearchableRepository	repository;

	private String						error	= null;
	private Set<ResourceDescriptor>		directResources;
	private Set<ResourceDescriptor>		indirectResources;

	public QueryJpmDependenciesRunnable(URI origin, SearchableRepository repository) {
		this.origin = origin;
		this.repository = repository;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		SubMonitor progress = SubMonitor.convert(monitor, 5);
		progress.setTaskName("Querying dependencies...");

		try {
			Set<ResourceDescriptor> resources = repository.getResources(origin, true);
			directResources = new HashSet<>();
			indirectResources = new HashSet<>();

			for (ResourceDescriptor resource : resources) {
				if (resource.dependency)
					indirectResources.add(resource);
				else
					directResources.add(resource);
			}
			progress.worked(5);
		} catch (Exception e) {
			error = "Error searching repository: " + e.getMessage();
		}
	}

	public String getError() {
		return error;
	}

	public Set<ResourceDescriptor> getDirectResources() {
		return directResources;
	}

	public Set<ResourceDescriptor> getIndirectResources() {
		return indirectResources;
	}

}
