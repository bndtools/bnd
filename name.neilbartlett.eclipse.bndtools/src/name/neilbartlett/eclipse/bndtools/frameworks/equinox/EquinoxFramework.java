package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class EquinoxFramework implements IFramework {

	public IFrameworkInstance createFrameworkInstance(File resource) throws CoreException {
		IPath instancePath = new Path(resource.getAbsolutePath());
		return new EquinoxInstance(instancePath);
	}
}

