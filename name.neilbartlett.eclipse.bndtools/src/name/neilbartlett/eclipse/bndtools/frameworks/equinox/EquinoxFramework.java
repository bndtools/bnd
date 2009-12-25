package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class EquinoxFramework implements IFramework {

	static final String EQUINOX_STARTER = "org.eclipse.core.runtime.adaptor.EclipseStarter";
	static final String STANDARD_ARGS = "-console -consoleLog";

	public IFrameworkInstance createFrameworkInstance(File resource) throws CoreException {
		return new EquinoxInstance(new Path(resource.getAbsolutePath()));
	}

	public String getMainClassName() {
		return EQUINOX_STARTER;
	}

}

