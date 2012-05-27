package test.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.osgi.service.log.LogService;

import aQute.bnd.service.Registry;
import aQute.lib.deployer.repository.api.CheckResult;
import aQute.lib.deployer.repository.api.Decision;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.deployer.repository.api.IRepositoryListener;

class NonGeneratingProvider implements IRepositoryContentProvider {

	public String getName() {
		return "Nongenerating";
	}

	public void parseIndex(InputStream stream, String baseUrl, IRepositoryListener listener, LogService log) throws Exception {
	}

	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		return new CheckResult(Decision.accept, "I accept anything but create nothing!", null);
	}

	public boolean supportsGeneration() {
		return false;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty, Registry registry, LogService log) throws Exception {
		throw new UnsupportedOperationException("I told you I don't support this!");
	}

	public String getDefaultIndexName(boolean pretty) {
		return "neverhappens.xml";
	}

}
