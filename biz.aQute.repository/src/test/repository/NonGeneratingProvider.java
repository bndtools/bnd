package test.repository;

import java.io.*;
import java.util.*;

import org.osgi.service.log.*;

import aQute.bnd.service.*;
import aQute.lib.deployer.repository.api.*;

class NonGeneratingProvider implements IRepositoryContentProvider {

	public String getName() {
		return "Nongenerating";
	}

	public void parseIndex(InputStream stream, String baseUrl, IRepositoryListener listener, LogService log)
			throws Exception {}

	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		return new CheckResult(Decision.accept, "I accept anything but create nothing!", null);
	}

	public boolean supportsGeneration() {
		return false;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty,
			Registry registry, LogService log) throws Exception {
		throw new UnsupportedOperationException("I told you I don't support this!");
	}

	public String getDefaultIndexName(boolean pretty) {
		return "neverhappens.xml";
	}

}
