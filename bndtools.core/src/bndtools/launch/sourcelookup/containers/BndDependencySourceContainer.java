package bndtools.launch.sourcelookup.containers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunMode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;

import aQute.bnd.build.Run;
import aQute.lib.exceptions.Exceptions;
import bndtools.launch.util.LaunchUtils;

public class BndDependencySourceContainer extends CompositeSourceContainer {
	private static final ILogger	logger	= Logger.getLogger(BndDependencySourceContainer.class);

	public static final String		TYPE_ID	= "org.bndtools.core.launch.sourceContainerTypes.bndDependencies";

	private Run						lastRun	= null;

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BndDependencySourceContainer;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	protected ILaunchConfiguration getLaunchConfiguration() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.getLaunchConfiguration();
		}
		return null;
	}

	@Override
	public String getName() {
		return "Bnd Dependencies";
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	static boolean hasChildren(ISourceContainer container) {
		try {
			ISourceContainer[] children = container.getSourceContainers();
			return children != null && children.length > 0;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	final private static ISourceContainer[] EMPTY_SOURCE = new ISourceContainer[0];

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration();
		Set<String> projectsAdded = new HashSet<>();
		try {
			if (lastRun != null) {
				LaunchUtils.endRun(lastRun);
			}

			Run run = LaunchUtils.createRun(config, RunMode.SOURCES);
			if (run != null) {
				ISourceContainer[] result = Stream
					.concat(Stream.of(new BndrunEESourceContainer(run)), Stream.of("runfw", "runpath", "runbundles")
						.map(directive -> new BndrunDirectiveSourceContainer(run, directive))
						.filter(BndDependencySourceContainer::hasChildren))
					.toArray(ISourceContainer[]::new);
				lastRun = run;
				return result;
			}
		} catch (Exception e) {
			logger.logError("Error querying Bnd dependency source containers.", e);
		}
		return EMPTY_SOURCE;
	}

	@Override
	public void dispose() {
		super.dispose();

		if (lastRun != null) {
			LaunchUtils.endRun(lastRun);
			lastRun = null;
		}
	}
}
