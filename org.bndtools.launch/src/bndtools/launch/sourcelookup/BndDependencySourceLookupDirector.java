package bndtools.launch.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import bndtools.launch.sourcelookup.containers.BndDependencySourceContainer;

@Component(scope = ServiceScope.PROTOTYPE, service = ISourceLookupDirector.class)
public class BndDependencySourceLookupDirector extends AbstractSourceLookupDirector {

	@Override
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] {
			new JavaSourceLookupParticipant()
		});
	}

	@Override
	public synchronized ISourceContainer[] getSourceContainers() {
		return super.getSourceContainers();
	}

	@Override
	public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
		dispose();
		setLaunchConfiguration(configuration);
		setSourceContainers(new ISourceContainer[] {
			new BndDependencySourceContainer(), new DefaultSourceContainer()
		});
		initializeParticipants();
	}
}
