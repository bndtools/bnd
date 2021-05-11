package aQute.bnd.build.api;

import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Packages;

public interface ArtifactInfo {
	BundleId getBundleId();

	Packages getExports();

	Packages getImports();

	Packages getContained();

}
