package bndtools.launch.sourcelookup.containers;

import static aQute.bnd.osgi.Constants.BSN_SOURCE_SUFFIX;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;

import aQute.bnd.build.Container;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.version.Version;
import bndtools.central.RepositoryUtils;

public class BundleSourceContainer extends AbstractSourceContainer {
	private static final ILogger				logger		= Logger.getLogger(BundleSourceContainer.class);

	public static final String					TYPE_ID		= "org.bndtools.core.launch.sourceContainerTypes.bundle";

	private final Container						bundle;

	private AtomicReference<ISourceContainer>	delegate	= new AtomicReference<>();
	private AtomicReference<String>				error		= new AtomicReference<>();
	private CountDownLatch						flag		= new CountDownLatch(1);

	public BundleSourceContainer(Container bundle) {
		this.bundle = bundle;
		if (bundle == null) {
			throw new NullPointerException("bundle shouldn't be null");
		}
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

	@Override
	public String getName() {
		StringBuilder retval = new StringBuilder(100);
		retval.append(bundle.getBundleSymbolicName())
			.append(':')
			.append(bundle.getVersion());
		if (error.get() != null) {
			retval.append(" <error downloading source: ")
				.append(error.get())
				.append('>');
		}
		return retval.toString();
	}

	@Override
	public void init(ISourceLookupDirector director) {
		super.init(director);
		// Look for possible source bundle.
		final String sourceBSN = bundle.getBundleSymbolicName() + BSN_SOURCE_SUFFIX;
		final Version version = new Version(bundle.getVersion());
		for (RepositoryPlugin repo : RepositoryUtils.listRepositories(true)) {
			if (repo == null || repo instanceof WorkspaceRepository) {
				continue;
			}
			try {
				File sourceBundle = repo.get(sourceBSN, version, Collections.emptyMap(), new DownloadListener() {
					@Override
					public void success(File file) {
						delegate.compareAndSet(null, new ExternalArchiveSourceContainer(file.toString(), false));
						flag.countDown();
					}

					@Override
					public void failure(File file, String reason) {
						logger.logError("Error trying to download " + file + ": " + reason, null);
						error.set(reason);
						flag.countDown();
					}

					@Override
					public boolean progress(File file, int percentage) {
						return true;
					}

				});
				if (sourceBundle != null) {
					// Bundle may not yet have finished downloading; don't
					// construct the delegate yet, the downloadlistener callback
					// will handle that.
					return;
				}
			} catch (Exception e) {
				logger.logError("Error trying to fetch " + sourceBSN + ':' + version + " from " + repo.getName(), e);
			}
		}

		// If we've gotten this far no separate source bundle was found, so use
		// the bundle itself.
		delegate.set(new ExternalArchiveSourceContainer(bundle.getFile()
			.toString(), false));
		flag.countDown();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	final private static Object[] EMPTY = new Object[0];

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		try {
			flag.await(1000, TimeUnit.MILLISECONDS);
			if (delegate.get() == null) {
				return EMPTY;
			}
			return delegate.get()
				.findSourceElements(name);
		} catch (InterruptedException e) {
			logger.logInfo("Time out waiting for source to finish downloading: " + getName(), e);
			return EMPTY;
		}
	}
}
