package bndtools.facades.jdt;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.osgi.annotation.versioning.ConsumerType;

import bndtools.facades.util.EclipseBinder;

@ConsumerType
public class IClasspathContainerFacade extends EclipseBinder<IClasspathContainer>
	implements IClasspathContainer {

	public interface Delegate extends IClasspathContainer {}

	public IClasspathContainerFacade() {
		super(IClasspathContainer.class, null);
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		return get()
			.getClasspathEntries();
	}

	@Override
	public String getDescription() {
		return get()
			.getDescription();
	}

	@Override
	public int getKind() {
		return get()
			.getKind();
	}

	@Override
	public IPath getPath() {
		return get()
			.getPath();
	}
}
