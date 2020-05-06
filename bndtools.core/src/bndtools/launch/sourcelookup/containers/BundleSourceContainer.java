package bndtools.launch.sourcelookup.containers;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;

import aQute.bnd.build.Container;

public class BundleSourceContainer extends AbstractSourceContainer {
	private static final ILogger	logger	= Logger.getLogger(BundleSourceContainer.class);

	public static final String		TYPE_ID	= "org.bndtools.core.launch.sourceContainerTypes.bundle";

	private final Container			bundle;

	private final ISourceContainer	delegate;

	public BundleSourceContainer(Container bundle) {
		this.bundle = bundle;
		if (bundle == null) {
			throw new NullPointerException("bundle shouldn't be null");
		}
		delegate = new ExternalArchiveSourceContainer(bundle.getFile()
			.toString(), false);
	}

	public Container getBundle() {
		return bundle;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BundleSourceContainer && ((BundleSourceContainer) obj).getBundle()
			.equals(bundle);
	}

	@Override
	public int hashCode() {
		return bundle.hashCode();
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
		return bundle.getBundleSymbolicName() + ":" + bundle.getVersion();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		return delegate.findSourceElements(name);
	}
}
