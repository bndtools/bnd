package bndtools.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

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
			new DefaultSourceContainer(), new BndDependencySourceContainer()
		});
		initializeParticipants();
	}
}
