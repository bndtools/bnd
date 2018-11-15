package aQute.bnd.service.remoteworkspace;

import java.io.Closeable;
import java.util.List;

import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;

public interface RemoteWorkspace extends Closeable {

	String getBndVersion();

	RunSpecification getRun(String pathToBndOrBndrun);

	RunSpecification analyzeTestSetup(String projectDir);

	List<String> getLatestBundles(String projectDir, String specification);

	byte[] build(String projectPath, BuilderSpecification spec);

	List<String> getProjects();

}
