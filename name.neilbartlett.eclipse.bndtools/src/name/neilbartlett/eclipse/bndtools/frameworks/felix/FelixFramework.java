package name.neilbartlett.eclipse.bndtools.frameworks.felix;

import java.io.File;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class FelixFramework implements IFramework {

	public IFrameworkInstance createFrameworkInstance(File resource)
			throws CoreException {
		return new FelixInstance(new Path(resource.getAbsolutePath()));
	}

}
