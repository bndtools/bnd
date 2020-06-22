package bndtools.launch.sourcelookup.containers;

import java.util.stream.Stream;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import aQute.bnd.build.Run;
import bndtools.launch.api.AbstractOSGiLaunchDelegate;

public class BndrunEESourceContainer extends CompositeSourceContainer {
	private static final ILogger	logger	= Logger.getLogger(BndrunEESourceContainer.class);

	public static final String		TYPE_ID	= "org.bndtools.core.launch.sourceContainerTypes.bndrunEE";

	private Run						run;

	private IVMInstall				vm;

	public BndrunEESourceContainer(Run run) {
		if (run == null) {
			throw new NullPointerException("run should not be null");
		}
		this.run = run;
	}

	@Override
	public void init(ISourceLookupDirector director) {
		super.init(director);
		try {
			// This must be deferred until after the director is initialized
			vm = AbstractOSGiLaunchDelegate.getVMInstall(director.getLaunchConfiguration(), run);
		} catch (CoreException e) {}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BndrunEESourceContainer;
	}

	@Override
	public int hashCode() {
		return BndrunEESourceContainer.class.hashCode();
	}

	@Override
	public String getName() {
		return "runee: " + run.getRunee() + " [" + getVMDesc() + ']';
	}

	public String getVMDesc() {
		if (vm == null) {
			return "<no VM found>";
		}
		return vm.getName();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	final private static ISourceContainer[] EMPTY_SOURCE = new ISourceContainer[0];

	@Override
	protected ISourceContainer[] createSourceContainers() {
		try {
			LibraryLocation[] libs = vm.getLibraryLocations();
			if (libs == null) {
				libs = JavaRuntime.getLibraryLocations(vm);
			}
			if (libs == null) {
				logger.logError("No library locations found for JVM: " + vm.getName(), null);
				return EMPTY_SOURCE;
			}
			return Stream.of(libs)
				.map(location -> (location.getSystemLibrarySourcePath() == null || location.getSystemLibrarySourcePath()
					.isEmpty()) ? location.getSystemLibraryPath() : location.getSystemLibrarySourcePath())
				.map(IPath::toOSString)
				.distinct() // For some JREs, most libraries have their source
							// stored in src.zip so without distinct() src.zip
							// will appear many times.
				.map(path -> new ExternalArchiveSourceContainer(path, false))
				.toArray(ISourceContainer[]::new);
		} catch (Exception e) {
			logger.logError("Error querying Bnd JVM source containers", e);
		}

		return EMPTY_SOURCE;
	}

	@Override
	public void dispose() {
		super.dispose();
		// Help the garbage collector out a little; this should not be necessary
		run = null;
		vm = null;
	}
}
